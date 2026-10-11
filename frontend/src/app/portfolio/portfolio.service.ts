import { HttpClient, HttpResourceRef, httpResource } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { FIRST_PAGE, PageResponse } from '../core/page-response';
import { CurrencyView } from '../money/money-models';
import {
  CreatePortfolioRequest,
  PortfolioView,
  TransferDirection,
  TransferQuoteView,
  TransferRequest,
  TransferView,
} from './portfolio-models';

export const PORTFOLIOS_PATH = '/portfolios';
export const TRANSFERS_PATH = '/transfers';
export const QUOTE_PATH = '/quote';
export const CURRENCIES_PATH = '/currencies';
export const RECENT_TRANSFERS = 20;

export const PortfolioParam = {
  Direction: 'direction',
  Amount: 'amount',
  Page: 'page',
  Size: 'size',
} as const;

export interface QuoteQuery {
  readonly portfolioId: number;
  readonly direction: TransferDirection;
  readonly amount: number;
}

@Injectable({ providedIn: 'root' })
export class PortfolioService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}${PORTFOLIOS_PATH}`;

  portfolios(): HttpResourceRef<readonly PortfolioView[] | undefined> {
    return httpResource<readonly PortfolioView[]>(() => this.baseUrl);
  }

  portfolio(portfolioId: () => number): HttpResourceRef<PortfolioView | undefined> {
    return httpResource<PortfolioView>(() => this.portfolioUrl(portfolioId()));
  }

  currencies(): HttpResourceRef<readonly CurrencyView[] | undefined> {
    return httpResource<readonly CurrencyView[]>(() => `${environment.apiUrl}${CURRENCIES_PATH}`);
  }

  recentTransfers(
    portfolioId: () => number,
  ): HttpResourceRef<PageResponse<TransferView> | undefined> {
    return httpResource<PageResponse<TransferView>>(() => ({
      url: `${this.portfolioUrl(portfolioId())}${TRANSFERS_PATH}`,
      params: { [PortfolioParam.Page]: FIRST_PAGE, [PortfolioParam.Size]: RECENT_TRANSFERS },
    }));
  }

  quote(query: () => QuoteQuery | undefined): HttpResourceRef<TransferQuoteView | undefined> {
    return httpResource<TransferQuoteView>(() => {
      const current = query();
      if (current === undefined) {
        return undefined;
      }
      return {
        url: `${this.portfolioUrl(current.portfolioId)}${TRANSFERS_PATH}${QUOTE_PATH}`,
        params: {
          [PortfolioParam.Direction]: current.direction,
          [PortfolioParam.Amount]: current.amount,
        },
      };
    });
  }

  create(request: CreatePortfolioRequest): Observable<PortfolioView> {
    return this.http.post<PortfolioView>(this.baseUrl, request);
  }

  transfer(portfolioId: number, request: TransferRequest): Observable<TransferView> {
    return this.http.post<TransferView>(
      `${this.portfolioUrl(portfolioId)}${TRANSFERS_PATH}`,
      request,
    );
  }

  private portfolioUrl(portfolioId: number): string {
    return `${this.baseUrl}/${portfolioId}`;
  }
}
