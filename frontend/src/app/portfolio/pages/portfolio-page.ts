import { DatePipe, DecimalPipe } from '@angular/common';
import { HttpErrorResponse, HttpStatusCode } from '@angular/common/http';
import { Component, computed, inject, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AccountService } from '../../account/account.service';
import { AppPath } from '../../core/app-routes';
import { DATE_TIME_FORMAT } from '../../core/formats';
import { loadedValue } from '../../core/loaded-value';
import { MoneyPipe } from '../../money/money.pipe';
import { PerformanceResult } from '../../money/performance-result';
import { DIRECTION_LABELS } from '../portfolio-models';
import { PortfolioService } from '../portfolio.service';
import { RATE_FORMAT, TransferForm } from '../transfer-form';

@Component({
  selector: 'app-portfolio-page',
  imports: [DatePipe, DecimalPipe, RouterLink, MoneyPipe, PerformanceResult, TransferForm],
  templateUrl: './portfolio-page.html',
  styleUrl: './portfolio-page.scss',
})
export class PortfolioPage {
  readonly id = input.required<string>();

  private readonly portfolios = inject(PortfolioService);
  private readonly accounts = inject(AccountService);

  protected readonly paths = AppPath;
  protected readonly dateFormat = DATE_TIME_FORMAT;
  protected readonly rateFormat = RATE_FORMAT;
  protected readonly directionLabels = DIRECTION_LABELS;

  private readonly portfolioId = computed(() => Number(this.id()));

  protected readonly portfolio = this.portfolios.portfolio(this.portfolioId);
  protected readonly account = this.accounts.account();
  protected readonly transfers = this.portfolios.recentTransfers(this.portfolioId);

  protected readonly portfolioView = loadedValue(this.portfolio);
  protected readonly mainAccount = loadedValue(this.account);
  protected readonly transferPage = loadedValue(this.transfers);
  protected readonly notFound = computed(() => {
    const error = this.portfolio.error();
    return error instanceof HttpErrorResponse && error.status === HttpStatusCode.NotFound;
  });

  protected reload(): void {
    this.portfolio.reload();
    this.account.reload();
    this.transfers.reload();
  }
}
