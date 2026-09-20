import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { RouterLink } from '@angular/router';
import { APP_ACCOUNT_NOTICE } from '../../core/app-info';
import { AppPath } from '../../core/app-routes';
import { Failure, toFailure } from '../../core/error-response';
import { UserStatus } from '../../core/user-status';
import {
  DISPLAY_NAME_MAX_LENGTH,
  EMAIL_MAX_LENGTH,
  PASSWORD_MAX_LENGTH,
  PASSWORD_MIN_LENGTH,
} from '../auth-rules';
import { AuthService, RegisterResponse } from '../auth.service';
import { PasswordVisibilityToggle } from '../../shared/password-visibility-toggle';

export const PASSWORDS_MISMATCH = 'passwordsMismatch';

const RegisterField = {
  Email: 'email',
  DisplayName: 'displayName',
  Password: 'password',
  ConfirmPassword: 'confirmPassword',
} as const;

const PasswordFieldType = {
  Hidden: 'password',
  Visible: 'text',
} as const;

function passwordsMatch(group: AbstractControl): ValidationErrors | null {
  const password = group.get(RegisterField.Password)?.value;
  const confirmation = group.get(RegisterField.ConfirmPassword)?.value;
  return password === confirmation ? null : { [PASSWORDS_MISMATCH]: true };
}

@Component({
  selector: 'app-register-page',
  imports: [ReactiveFormsModule, RouterLink, PasswordVisibilityToggle],
  templateUrl: './register-page.html',
  styleUrl: './register-page.scss',
})
export class RegisterPage {
  private readonly auth = inject(AuthService);
  private readonly formBuilder = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly paths = AppPath;
  protected readonly accountNotice = APP_ACCOUNT_NOTICE;
  protected readonly emailMaxLength = EMAIL_MAX_LENGTH;
  protected readonly displayNameMaxLength = DISPLAY_NAME_MAX_LENGTH;
  protected readonly passwordMinLength = PASSWORD_MIN_LENGTH;
  protected readonly passwordMaxLength = PASSWORD_MAX_LENGTH;
  protected readonly mismatchError = PASSWORDS_MISMATCH;

  protected readonly form = this.formBuilder.nonNullable.group(
    {
      email: ['', [Validators.required, Validators.email, Validators.maxLength(EMAIL_MAX_LENGTH)]],
      displayName: ['', [Validators.required, Validators.maxLength(DISPLAY_NAME_MAX_LENGTH)]],
      password: [
        '',
        [
          Validators.required,
          Validators.minLength(PASSWORD_MIN_LENGTH),
          Validators.maxLength(PASSWORD_MAX_LENGTH),
        ],
      ],
      confirmPassword: ['', [Validators.required]],
    },
    { validators: passwordsMatch },
  );

  protected readonly submitting = signal(false);
  protected readonly created = signal<RegisterResponse | null>(null);
  protected readonly failure = signal<Failure | null>(null);
  protected readonly passwordsVisible = signal(false);

  protected readonly passwordFieldType = computed(() =>
    this.passwordsVisible() ? PasswordFieldType.Visible : PasswordFieldType.Hidden,
  );
  protected readonly awaitingVerification = computed(
    () => this.created()?.status === UserStatus.PendingVerification,
  );
  protected readonly emailFailure = computed(() => this.failureFor(RegisterField.Email));
  protected readonly displayNameFailure = computed(() =>
    this.failureFor(RegisterField.DisplayName),
  );
  protected readonly passwordFailure = computed(() => this.failureFor(RegisterField.Password));

  protected togglePasswordVisibility(): void {
    this.passwordsVisible.update((visible) => !visible);
  }

  protected submit(): void {
    if (this.submitting()) {
      return;
    }
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.submitting.set(true);
    this.failure.set(null);
    this.created.set(null);

    const { email, displayName, password } = this.form.getRawValue();

    this.auth
      .register({ email, displayName, password })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (response) => {
          this.created.set(response);
          this.submitting.set(false);
          this.form.reset();
        },
        error: (error: HttpErrorResponse) => {
          this.failure.set(toFailure(error));
          this.submitting.set(false);
        },
      });
  }

  private failureFor(field: string): string | null {
    return this.failure()?.fieldErrors[field] ?? null;
  }
}
