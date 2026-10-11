import { DatePipe } from '@angular/common';
import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ENTRY_LABELS } from '../../account/account-models';
import { AccountService } from '../../account/account.service';
import { AuthService } from '../../auth/auth.service';
import { AppPath, portfolioPath } from '../../core/app-routes';
import { DATE_TIME_FORMAT } from '../../core/formats';
import { loadedValue } from '../../core/loaded-value';
import { MoneyPipe } from '../../money/money.pipe';
import { PerformanceResult } from '../../money/performance-result';
import { PortfolioService } from '../../portfolio/portfolio.service';

@Component({
  selector: 'app-dashboard-page',
  imports: [DatePipe, RouterLink, MoneyPipe, PerformanceResult],
  templateUrl: './dashboard-page.html',
  styleUrl: './dashboard-page.scss',
})
export class DashboardPage {
  private readonly auth = inject(AuthService);
  private readonly accounts = inject(AccountService);
  private readonly portfolioService = inject(PortfolioService);

  protected readonly user = this.auth.currentUser;
  protected readonly isAdmin = this.auth.isAdmin;
  protected readonly paths = AppPath;
  protected readonly portfolioPath = portfolioPath;
  protected readonly dateFormat = DATE_TIME_FORMAT;
  protected readonly entryLabels = ENTRY_LABELS;

  protected readonly account = this.accounts.account();
  protected readonly entries = this.accounts.recentEntries();
  protected readonly portfolios = this.portfolioService.portfolios();

  protected readonly mainAccount = loadedValue(this.account);
  protected readonly entryPage = loadedValue(this.entries);
  protected readonly portfolioList = loadedValue(this.portfolios);
}
