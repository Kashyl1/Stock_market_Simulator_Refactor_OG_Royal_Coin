import { Performance } from '../money/money-models';

export const WalletEntryType = {
  Deposit: 'DEPOSIT',
  Withdrawal: 'WITHDRAWAL',
  TransferIn: 'TRANSFER_IN',
  TransferOut: 'TRANSFER_OUT',
  TradeDebit: 'TRADE_DEBIT',
  TradeCredit: 'TRADE_CREDIT',
  Fee: 'FEE',
  Reserve: 'RESERVE',
  Release: 'RELEASE',
  Income: 'INCOME',
} as const;

export type WalletEntryType = (typeof WalletEntryType)[keyof typeof WalletEntryType];

export const ENTRY_LABELS: Readonly<Record<WalletEntryType, string>> = {
  [WalletEntryType.Deposit]: 'Virtual money granted',
  [WalletEntryType.Withdrawal]: 'Withdrawal',
  [WalletEntryType.TransferIn]: 'Back from',
  [WalletEntryType.TransferOut]: 'Moved to',
  [WalletEntryType.TradeDebit]: 'Purchase',
  [WalletEntryType.TradeCredit]: 'Sale',
  [WalletEntryType.Fee]: 'Fee',
  [WalletEntryType.Reserve]: 'Reserved',
  [WalletEntryType.Release]: 'Released',
  [WalletEntryType.Income]: 'Income',
};

export interface AccountView {
  readonly currency: string;
  readonly cashBalance: number;
  readonly available: number;
  readonly performance: Performance;
}

export interface AccountEntryView {
  readonly id: number;
  readonly type: WalletEntryType;
  readonly amount: number;
  readonly currency: string;
  readonly balanceAfter: number;
  readonly effectiveAt: string;
  readonly portfolioId: number | null;
  readonly portfolioName: string | null;
}
