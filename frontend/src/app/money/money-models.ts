export interface Performance {
  readonly currency: string;
  readonly value: number | null;
  readonly putIn: number;
  readonly takenOut: number;
  readonly netDeposits: number;
  readonly result: number | null;
  readonly resultPercent: number | null;
}

export interface CurrencyView {
  readonly code: string;
  readonly name: string;
  readonly minorUnits: number;
}
