import { TestBed } from '@angular/core/testing';
import { MISSING_AMOUNT, MoneyPipe } from './money.pipe';

const ZLOTY = 'PLN';
const YEN = 'JPY';

describe('MoneyPipe', () => {
  let pipe: MoneyPipe;

  beforeEach(() => {
    pipe = TestBed.runInInjectionContext(() => new MoneyPipe());
  });

  it('writes an amount with the decimal places of its currency', () => {
    expect(pipe.transform(1000, ZLOTY)).toBe('1,000.00 PLN');
    expect(pipe.transform(402, YEN)).toBe('402 JPY');
  });

  it('shows the sign of a result when asked', () => {
    expect(pipe.transform(2.97, ZLOTY, true)).toBe('+2.97 PLN');
    expect(pipe.transform(-10.64, ZLOTY, true)).toBe('-10.64 PLN');
    expect(pipe.transform(0, ZLOTY, true)).toBe('0.00 PLN');
  });

  it('marks an amount that is not known', () => {
    expect(pipe.transform(null, ZLOTY)).toBe(MISSING_AMOUNT);
    expect(pipe.transform(undefined, ZLOTY)).toBe(MISSING_AMOUNT);
  });
});
