import { Component, DestroyRef, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from './auth/auth.service';
import { APP_DISCLAIMER, APP_NAME } from './core/app-info';
import { AppPath } from './core/app-routes';

@Component({
  selector: 'app-root',
  imports: [RouterLink, RouterLinkActive, RouterOutlet],
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly appName = APP_NAME;
  protected readonly disclaimer = APP_DISCLAIMER;
  protected readonly paths = AppPath;
  protected readonly currentYear = new Date().getFullYear();
  protected readonly currentUser = this.auth.currentUser;

  protected signOut(): void {
    this.auth
      .logout()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => this.router.navigate([AppPath.Home]),
        error: () => this.router.navigate([AppPath.Home]),
      });
  }
}
