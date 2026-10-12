import { environment } from '../../environments/environment';
import { AccountEntryView, AccountView, WalletEntryType } from '../account/account-models';
import { CurrencyView, Performance } from '../money/money-models';
import {
  PortfolioView,
  TransferDirection,
  TransferQuoteView,
  TransferView,
} from '../portfolio/portfolio-models';

const MOVED_AT = '2026-10-08T10:00:00Z';
const CENTS = 2;

export const TEST_MAIN_CURRENCY = 'PLN';
export const TEST_PORTFOLIO_CURRENCY = 'USD';
export const TEST_PORTFOLIO_ID = 700;
export const TEST_PORTFOLIO_NAME = "Troodon's dollars";
export const TEST_STARTING_BALANCE = 1000;
export const TEST_DEPOSITED = 400;
export const TEST_ARRIVED = 99.5;

export const TEST_PORTFOLIO_PERFORMANCE: Performance = {
  currency: TEST_MAIN_CURRENCY,
  value: 389.36,
  putIn: TEST_DEPOSITED,
  takenOut: 0,
  netDeposits: TEST_DEPOSITED,
  result: -10.64,
  resultPercent: -2.66,
};

export const TEST_MAIN_ACCOUNT: AccountView = {
  currency: TEST_MAIN_CURRENCY,
  cashBalance: TEST_STARTING_BALANCE - TEST_DEPOSITED,
  available: TEST_STARTING_BALANCE - TEST_DEPOSITED,
  performance: {
    currency: TEST_MAIN_CURRENCY,
    value: 989.36,
    putIn: TEST_STARTING_BALANCE,
    takenOut: 0,
    netDeposits: TEST_STARTING_BALANCE,
    result: -10.64,
    resultPercent: -1.06,
  },
};

export const TEST_PORTFOLIO: PortfolioView = {
  id: TEST_PORTFOLIO_ID,
  name: TEST_PORTFOLIO_NAME,
  currency: TEST_PORTFOLIO_CURRENCY,
  cashBalance: TEST_ARRIVED,
  available: TEST_ARRIVED,
  performance: TEST_PORTFOLIO_PERFORMANCE,
  createdAt: MOVED_AT,
};

export const TEST_QUOTE: TransferQuoteView = {
  direction: TransferDirection.In,
  amount: TEST_ARRIVED,
  currency: TEST_PORTFOLIO_CURRENCY,
  mainAmount: TEST_DEPOSITED,
  mainCurrency: TEST_MAIN_CURRENCY,
  fxRate: 0.25,
  fxFeeAmount: 2,
};

export const TEST_TRANSFER: TransferView = { ...TEST_QUOTE, id: 7000, transferredAt: MOVED_AT };

export const TEST_DEPOSIT_ENTRY: AccountEntryView = {
  id: 1,
  type: WalletEntryType.Deposit,
  amount: TEST_STARTING_BALANCE,
  currency: TEST_MAIN_CURRENCY,
  balanceAfter: TEST_STARTING_BALANCE,
  effectiveAt: MOVED_AT,
  portfolioId: null,
  portfolioName: null,
};

export const TEST_TRANSFER_ENTRY: AccountEntryView = {
  id: 2,
  type: WalletEntryType.TransferOut,
  amount: -TEST_DEPOSITED,
  currency: TEST_MAIN_CURRENCY,
  balanceAfter: TEST_STARTING_BALANCE - TEST_DEPOSITED,
  effectiveAt: MOVED_AT,
  portfolioId: TEST_PORTFOLIO_ID,
  portfolioName: TEST_PORTFOLIO_NAME,
};

export const TEST_CURRENCIES: readonly CurrencyView[] = [
  { code: TEST_MAIN_CURRENCY, name: 'Polish zloty', minorUnits: CENTS },
  { code: TEST_PORTFOLIO_CURRENCY, name: 'US dollar', minorUnits: CENTS },
];

export function apiUrl(path: string): string {
  return `${environment.apiUrl}${path}`;
}
