import { formatNumber } from '@angular/common';
import { Component, LOCALE_ID, computed, inject, input } from '@angular/core';
import { MoneyPipe, PLUS_SIGN } from './money.pipe';
import { Performance } from './money-models';

const PERCENT_FORMAT = '1.2-2';
export const TONE_CLASS_PREFIX = 'result--';

export const ResultTone = {
  Gain: 'gain',
  Loss: 'loss',
  Flat: 'flat',
} as const;

export type ResultTone = (typeof ResultTone)[keyof typeof ResultTone];

@Component({
  selector: 'app-performance-result',
  imports: [MoneyPipe],
  templateUrl: './performance-result.html',
  styleUrl: './performance-result.scss',
})
export class PerformanceResult {
  readonly performance = input.required<Performance>();

  private readonly locale = inject(LOCALE_ID);

  protected readonly tone = computed<ResultTone>(() => {
    const result = this.performance().result ?? 0;
    if (result > 0) {
      return ResultTone.Gain;
    }
    return result < 0 ? ResultTone.Loss : ResultTone.Flat;
  });

  protected readonly toneClass = computed(() => `${TONE_CLASS_PREFIX}${this.tone()}`);

  protected readonly percent = computed(() => {
    const percent = this.performance().resultPercent;
    if (percent === null) {
      return null;
    }
    const sign = percent > 0 ? PLUS_SIGN : '';
    return `${sign}${formatNumber(percent, this.locale, PERCENT_FORMAT)}%`;
  });
}
