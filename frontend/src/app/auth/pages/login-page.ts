import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AppPath } from '../../core/app-routes';
import { EMAIL_MAX_LENGTH, PASSWORD_MAX_LENGTH } from '../auth-rules';

export const SIGN_IN_UNAVAILABLE_MESSAGE =
  'Signing in is not live yet - the login endpoint ships with the next backend slice. Registration already works.';

@Component({
  selector: 'app-login-page',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './login-page.html',
  styleUrl: './login-page.scss',
})
export class LoginPage {
  private readonly formBuilder = inject(FormBuilder);

  protected readonly paths = AppPath;
  protected readonly emailMaxLength = EMAIL_MAX_LENGTH;
  protected readonly passwordMaxLength = PASSWORD_MAX_LENGTH;

  protected readonly form = this.formBuilder.nonNullable.group({
    email: ['', [Validators.required, Validators.email, Validators.maxLength(EMAIL_MAX_LENGTH)]],
    password: ['', [Validators.required, Validators.maxLength(PASSWORD_MAX_LENGTH)]],
  });

  protected readonly notice = signal<string | null>(null);

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.notice.set(null);
      return;
    }

    this.notice.set(SIGN_IN_UNAVAILABLE_MESSAGE);
  }
}
