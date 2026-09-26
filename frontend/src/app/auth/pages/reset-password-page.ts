import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, computed, inject, input, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AppPath } from '../../core/app-routes';
import { Failure, toFailure } from '../../core/error-response';
import {
  PasswordFieldType,
  PasswordVisibilityToggle,
} from '../../shared/password-visibility-toggle';
import { PASSWORD_MAX_LENGTH, PASSWORD_MIN_LENGTH } from '../auth-rules';
import { AuthService } from '../auth.service';
import { PASSWORDS_MISMATCH, passwordsMatch } from '../passwords-match';

export const MISSING_TOKEN_MESSAGE =
  'This link has no token. Ask for a new reset link and open the one from the e-mail.';

const ResetField = {
  NewPassword: 'newPassword',
  ConfirmPassword: 'confirmPassword',
} as const;

@Component({
  selector: 'app-reset-password-page',
  imports: [ReactiveFormsModule, RouterLink, PasswordVisibilityToggle],
  templateUrl: './reset-password-page.html',
  styleUrl: './reset-password-page.scss',
})
export class ResetPasswordPage {
  private readonly auth = inject(AuthService);
  private readonly formBuilder = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);

  readonly token = input<string>();

  protected readonly paths = AppPath;
  protected readonly passwordMinLength = PASSWORD_MIN_LENGTH;
  protected readonly passwordMaxLength = PASSWORD_MAX_LENGTH;
  protected readonly mismatchError = PASSWORDS_MISMATCH;
  protected readonly missingTokenMessage = MISSING_TOKEN_MESSAGE;

  protected readonly form = this.formBuilder.nonNullable.group(
    {
      newPassword: [
        '',
        [
          Validators.required,
          Validators.minLength(PASSWORD_MIN_LENGTH),
          Validators.maxLength(PASSWORD_MAX_LENGTH),
        ],
      ],
      confirmPassword: ['', [Validators.required]],
    },
    { validators: passwordsMatch(ResetField.NewPassword, ResetField.ConfirmPassword) },
  );

  protected readonly submitting = signal(false);
  protected readonly changed = signal(false);
  protected readonly failure = signal<Failure | null>(null);
  protected readonly passwordsVisible = signal(false);

  protected readonly linkIsUsable = computed(() => Boolean(this.token()));
  protected readonly passwordFieldType = computed(() =>
    this.passwordsVisible() ? PasswordFieldType.Visible : PasswordFieldType.Hidden,
  );

  protected togglePasswordVisibility(): void {
    this.passwordsVisible.update((visible) => !visible);
  }

  protected submit(): void {
    const token = this.token();
    if (this.submitting() || !token) {
      return;
    }
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.submitting.set(true);
    this.failure.set(null);

    this.auth
      .resetPassword(token, this.form.controls.newPassword.value)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.changed.set(true);
          this.submitting.set(false);
        },
        error: (error: HttpErrorResponse) => {
          this.failure.set(toFailure(error));
          this.submitting.set(false);
        },
      });
  }
}
