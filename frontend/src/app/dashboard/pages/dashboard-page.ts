import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../auth/auth.service';
import { AppPath } from '../../core/app-routes';

@Component({
  selector: 'app-dashboard-page',
  imports: [RouterLink],
  templateUrl: './dashboard-page.html',
  styleUrl: './dashboard-page.scss',
})
export class DashboardPage {
  private readonly auth = inject(AuthService);

  protected readonly account = this.auth.currentUser;
  protected readonly isAdmin = this.auth.isAdmin;
  protected readonly paths = AppPath;
}
