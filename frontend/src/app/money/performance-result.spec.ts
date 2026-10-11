import { TestBed } from '@angular/core/testing';
import { element } from '../testing/dom';
import { TEST_PORTFOLIO_PERFORMANCE } from '../testing/test-money';
import { Performance } from './money-models';
import { PerformanceResult, ResultTone, TONE_CLASS_PREFIX } from './performance-result';

const RESULT = '.result';
const PERFORMANCE = 'performance';
const GAIN: Performance = { ...TEST_PORTFOLIO_PERFORMANCE, result: 2.97, resultPercent: 0.74 };
const NOT_VALUED: Performance = {
  ...TEST_PORTFOLIO_PERFORMANCE,
  value: null,
  result: null,
  resultPercent: null,
};

describe('PerformanceResult', () => {
  function render(performance: Performance) {
    const fixture = TestBed.createComponent(PerformanceResult);
    fixture.componentRef.setInput(PERFORMANCE, performance);
    TestBed.tick();
    return fixture;
  }

  it('shows a loss in red with its percentage', () => {
    const result = element(render(TEST_PORTFOLIO_PERFORMANCE), RESULT);

    expect(result.textContent).toContain('-10.64 PLN');
    expect(result.textContent).toContain('(-2.66%)');
    expect(result.classList).toContain(TONE_CLASS_PREFIX + ResultTone.Loss);
  });

  it('shows a gain with a plus', () => {
    const result = element(render(GAIN), RESULT);

    expect(result.textContent).toContain('+2.97 PLN');
    expect(result.textContent).toContain('(+0.74%)');
    expect(result.classList).toContain(TONE_CLASS_PREFIX + ResultTone.Gain);
  });

  it('says when there is nothing to value yet', () => {
    const fixture = render(NOT_VALUED);

    expect((fixture.nativeElement as HTMLElement).querySelector(RESULT)).toBeNull();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Not valued yet');
  });
});
