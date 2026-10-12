import { Performance } from '../money/money-models';

export const PORTFOLIO_NAME_MAX_LENGTH = 100;
export const DEFAULT_PORTFOLIO_CURRENCY = 'PLN';

export const TransferDirection = {
  In: 'IN',
  Out: 'OUT',
} as const;

export type TransferDirection = (typeof TransferDirection)[keyof typeof TransferDirection];

export const DIRECTION_LABELS: Readonly<Record<TransferDirection, string>> = {
  [TransferDirection.In]: 'Deposit',
  [TransferDirection.Out]: 'Withdrawal',
};

export interface PortfolioView {
  readonly id: number;
  readonly name: string;
  readonly currency: string;
  readonly cashBalance: number;
  readonly available: number;
  readonly performance: Performance;
  readonly createdAt: string;
}

export interface CreatePortfolioRequest {
  readonly name: string;
  readonly currency: string;
}

export interface TransferRequest {
  readonly direction: TransferDirection;
  readonly amount: number;
}

export interface TransferQuoteView {
  readonly direction: TransferDirection;
  readonly amount: number;
  readonly currency: string;
  readonly mainAmount: number;
  readonly mainCurrency: string;
  readonly fxRate: number | null;
  readonly fxFeeAmount: number;
}

export interface TransferView extends TransferQuoteView {
  readonly id: number;
  readonly transferredAt: string;
}
