import { ComponentFixture, TestBed } from '@angular/core/testing';
import { element, elements } from '../../testing/dom';
import { JobProgress } from './job-progress';

const BAR = '.progress__bar';
const SUCCEEDED_SHARE = '.progress__succeeded';
const FAILED_SHARE = '.progress__failed';
const COUNTS = '.progress__counts';
const SUCCEEDED = 3;
const FAILED = 1;
const TOTAL = 8;
const NOTHING = 0;
const SUCCEEDED_WIDTH = '37.5%';
const FAILED_WIDTH = '12.5%';

describe('JobProgress', () => {
  it('splits the bar into what succeeded, what failed and what is left', () => {
    const fixture = render(TOTAL);

    expect(element(fixture, SUCCEEDED_SHARE).style.width).toBe(SUCCEEDED_WIDTH);
    expect(element(fixture, FAILED_SHARE).style.width).toBe(FAILED_WIDTH);
    expect(element(fixture, COUNTS).textContent).toContain(String(TOTAL));
  });

  it('shows only the counts while the total is unknown', () => {
    const fixture = render(null);

    expect(elements(fixture, BAR).length).toBe(NOTHING);
    expect(element(fixture, COUNTS).textContent).toContain(String(SUCCEEDED));
  });

  it('draws no bar for a job with nothing to do', () => {
    expect(elements(render(NOTHING), BAR).length).toBe(NOTHING);
  });

  function render(total: number | null): ComponentFixture<JobProgress> {
    const fixture = TestBed.createComponent(JobProgress);
    fixture.componentRef.setInput('succeeded', SUCCEEDED);
    fixture.componentRef.setInput('failed', FAILED);
    fixture.componentRef.setInput('total', total);
    TestBed.tick();
    return fixture;
  }
});
