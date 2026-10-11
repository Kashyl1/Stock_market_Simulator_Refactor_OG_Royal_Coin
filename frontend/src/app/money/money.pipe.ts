import { formatNumber, getNumberOfCurrencyDigits } from '@angular/common';
import { LOCALE_ID, Pipe, PipeTransform, inject } from '@angular/core';

export const MISSING_AMOUNT = '-';
export const PLUS_SIGN = '+';

@Pipe({ name: 'money' })
export class MoneyPipe implements PipeTransform {
  private readonly locale = inject(LOCALE_ID);

  transform(amount: number | null | undefined, currency: string, signed = false): string {
    if (amount === null || amount === undefined) {
      return MISSING_AMOUNT;
    }
    const digits = getNumberOfCurrencyDigits(currency);
    const number = formatNumber(amount, this.locale, `1.${digits}-${digits}`);
    const sign = signed && amount > 0 ? PLUS_SIGN : '';
    return `${sign}${number} ${currency}`;
  }
}
