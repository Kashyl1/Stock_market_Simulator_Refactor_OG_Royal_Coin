import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AppPath } from '../../core/app-routes';
import { Failure, toFailure } from '../../core/error-response';
import { EMAIL_MAX_LENGTH } from '../auth-rules';
import { AuthService } from '../auth.service';

export const RESET_REQUESTED_MESSAGE =
  'If that address belongs to an account, a reset link is on its way. The link works once and expires after an hour.';

@Component({
  selector: 'app-forgot-password-page',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './forgot-password-page.html',
  styleUrl: './forgot-password-page.scss',
})
export class ForgotPasswordPage {
  private readonly auth = inject(AuthService);
  private readonly formBuilder = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly paths = AppPath;
  protected readonly emailMaxLength = EMAIL_MAX_LENGTH;
  protected readonly requestedMessage = RESET_REQUESTED_MESSAGE;

  protected readonly form = this.formBuilder.nonNullable.group({
    email: ['', [Validators.required, Validators.email, Validators.maxLength(EMAIL_MAX_LENGTH)]],
  });

  protected readonly submitting = signal(false);
  protected readonly requested = signal(false);
  protected readonly failure = signal<Failure | null>(null);

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

    this.auth
      .requestPasswordReset(this.form.controls.email.value)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.requested.set(true);
          this.submitting.set(false);
        },
        error: (error: HttpErrorResponse) => {
          this.failure.set(toFailure(error));
          this.submitting.set(false);
        },
      });
  }
}
