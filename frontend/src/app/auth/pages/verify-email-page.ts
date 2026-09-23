import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, OnInit, inject, input, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { AppPath } from '../../core/app-routes';
import { Failure, toFailure } from '../../core/error-response';
import { AuthService } from '../auth.service';

export const MISSING_TOKEN_MESSAGE = 'This link has no token. Open the link from the e-mail again.';

@Component({
  selector: 'app-verify-email-page',
  imports: [RouterLink],
  templateUrl: './verify-email-page.html',
  styleUrl: './verify-email-page.scss',
})
export class VerifyEmailPage implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly destroyRef = inject(DestroyRef);

  readonly token = input<string>();

  protected readonly paths = AppPath;
  protected readonly checking = signal(false);
  protected readonly verified = signal(false);
  protected readonly failure = signal<Failure | null>(null);

  ngOnInit(): void {
    const token = this.token();
    if (!token) {
      this.failure.set({ message: MISSING_TOKEN_MESSAGE, fieldErrors: {} });
      return;
    }

    this.checking.set(true);
    this.auth
      .verifyEmail(token)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.verified.set(true);
          this.checking.set(false);
        },
        error: (error: HttpErrorResponse) => {
          this.failure.set(toFailure(error));
          this.checking.set(false);
        },
      });
  }
}
