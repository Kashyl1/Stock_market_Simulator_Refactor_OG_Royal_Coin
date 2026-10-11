import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  TestRequest,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { FIRST_PAGE } from '../../../core/page-response';
import {
  buttonLabelled,
  element,
  elements,
  selectOption,
  submitForm,
  typeInto,
} from '../../../testing/dom';
import {
  FAILED_JOB,
  RUNNING_JOB,
  TEST_CONFLICT_MESSAGE,
  TEST_ADMIN,
  TEST_ENGINE,
  TEST_ENGINE_IDLE,
  TEST_ENGINE_OFF,
  TEST_TYPE,
  TEST_TYPE_CODE,
  batchUrl,
  onePage,
} from '../../../testing/test-batch';
import {
  ACKNOWLEDGE_PATH,
  BatchParam,
  ENGINE_PATH,
  ENGINE_PAUSE_PATH,
  ENGINE_RESUME_PATH,
  JOBS_PATH,
  STOP_PATH,
  TYPES_PATH,
} from '../batch-admin.service';
import {
  BatchEngineView,
  BatchJobStatus,
  BatchJobView,
  FINISHED_STATUSES,
  JOB_TABS,
  JobTab,
  REFRESH_TICK_MS,
} from '../batch-models';
import { BatchPage } from './batch-page';

const GET = 'GET';
const POST = 'POST';
const ENGINE = '.engine';
const TYPE_NAME = '.type h3';
const JOB_ROW = '.job';
const ERROR_NOTICE = '.notice--error';
const ENGINE_OFF = '.engine__off';
const ENGINE_ON_LABEL = 'Batch engine ON';
const ENGINE_OFF_LABEL = 'Batch engine OFF';
const TURN_OFF = 'Turn off';
const TURN_ON = 'Turn on';
const PLAN_TYPE = '#plan-type';
const PLAN_START = '#plan-start';
const PLAN_START_AT = '2026-10-12T08:00';
const SUCCESS_NOTICE = '.notice--success';
const FIELD_ERROR = '.field__error';
const NO_JOBS = 'No jobs here';
const STOP = 'Stop';
const ACKNOWLEDGE = 'Acknowledge';
const RUN_NOW = 'Run now';
const TRUE = 'true';
const ARIA_PRESSED = 'aria-pressed';
const CONFLICT = { status: 409, statusText: 'Conflict' };
const NOT_FOUND = { status: 404, statusText: 'Not Found' };
const NOT_FOUND_BODY = 'missing';
const ENGINE_UNAVAILABLE = 'not available';
const CONFLICT_BODY = {
  code: 'BATCH.JOB_NOT_RUNNING',
  message: TEST_CONFLICT_MESSAGE,
  status: CONFLICT.status,
  timestamp: '2026-10-10T10:31:00Z',
  path: '/api/admin/batch/jobs/2/acknowledge',
  fieldErrors: [],
};

describe('BatchPage', () => {
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [BatchPage],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    vi.useRealTimers();
    vi.restoreAllMocks();
    httpMock.verify();
  });

  it('shows the engine, the job types and every job by the name of its type', async () => {
    const fixture = await render([RUNNING_JOB]);

    expect(element(fixture, ENGINE).textContent).toContain(ENGINE_ON_LABEL);
    expect(element(fixture, TYPE_NAME).textContent).toContain(TEST_TYPE.name);
    expect(elements(fixture, JOB_ROW).length).toBe(1);
    expect(element(fixture, JOB_ROW).textContent).toContain(TEST_TYPE.name);
    expect(element(fixture, JOB_ROW).textContent).not.toContain(`#${RUNNING_JOB.id}`);
  });

  it('keeps showing the jobs when the engine status is not available', async () => {
    const fixture = TestBed.createComponent(BatchPage);
    TestBed.tick();
    httpMock.expectOne(batchUrl(ENGINE_PATH)).flush(NOT_FOUND_BODY, NOT_FOUND);
    httpMock.expectOne(batchUrl(TYPES_PATH)).flush([TEST_TYPE]);
    jobsRequest().flush(onePage([RUNNING_JOB]));
    await fixture.whenStable();

    expect(element(fixture, ENGINE).textContent).toContain(ENGINE_UNAVAILABLE);
    expect(elements(fixture, JOB_ROW).length).toBe(1);
  });

  it('asks for all jobs first and for the finished ones on their tab', async () => {
    const fixture = TestBed.createComponent(BatchPage);
    TestBed.tick();
    answerEngineAndTypes();
    const all = jobsRequest();
    expect(all.request.params.has(BatchParam.Status)).toBe(false);
    expect(all.request.params.get(BatchParam.Page)).toBe(String(FIRST_PAGE));
    all.flush(onePage([RUNNING_JOB]));
    await fixture.whenStable();

    tab(fixture, JobTab.Finished).click();
    TestBed.tick();
    const finished = jobsRequest();
    expect(finished.request.params.getAll(BatchParam.Status)).toEqual([...FINISHED_STATUSES]);
    finished.flush(onePage([]));
    await fixture.whenStable();

    expect(tab(fixture, JobTab.Finished).getAttribute(ARIA_PRESSED)).toBe(TRUE);
    expect((fixture.nativeElement as HTMLElement).textContent).toContain(NO_JOBS);
  });

  it('asks only for unread failure reports on the review tab', async () => {
    const fixture = await render([RUNNING_JOB]);

    tab(fixture, JobTab.ToReview).click();
    TestBed.tick();
    const review = jobsRequest();

    expect(review.request.params.get(BatchParam.Unacknowledged)).toBe(TRUE);
    expect(review.request.params.has(BatchParam.Status)).toBe(false);
    review.flush(onePage([FAILED_JOB]));
  });

  it('stops a running job once the admin confirms, then reloads', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    const fixture = await render([RUNNING_JOB]);

    buttonLabelled(fixture, STOP).click();
    const stop = httpMock.expectOne(batchUrl(`${JOBS_PATH}/${RUNNING_JOB.id}${STOP_PATH}`));
    expect(stop.request.method).toBe(POST);
    stop.flush(null);
    TestBed.tick();

    answerPageLoad([RUNNING_JOB]);
  });

  it('leaves a running job alone when the admin does not confirm', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(false);
    const fixture = await render([RUNNING_JOB]);

    buttonLabelled(fixture, STOP).click();

    httpMock.expectNone(batchUrl(`${JOBS_PATH}/${RUNNING_JOB.id}${STOP_PATH}`));
  });

  it('plans a job of a type with Run now', async () => {
    const fixture = await render([]);

    buttonLabelled(fixture, RUN_NOW).click();
    const plan = httpMock.expectOne(
      (request) => request.url === batchUrl(JOBS_PATH) && request.method === POST,
    );
    expect(plan.request.body).toEqual({ type: TEST_TYPE_CODE, scheduledFor: null });
    plan.flush(RUNNING_JOB);
    TestBed.tick();

    answerPageLoad([RUNNING_JOB]);
  });

  it('shows a red banner with who switched the engine off', async () => {
    const fixture = await render([], TEST_ENGINE_OFF);

    expect(element(fixture, ENGINE_OFF).textContent).toContain(ENGINE_OFF_LABEL);
    expect(element(fixture, ENGINE_OFF).textContent).toContain(TEST_ADMIN);
  });

  it('turns the engine off once the admin confirms', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    const fixture = await render([]);

    buttonLabelled(fixture, TURN_OFF).click();
    const pause = httpMock.expectOne(batchUrl(ENGINE_PAUSE_PATH));
    expect(pause.request.method).toBe(POST);
    pause.flush(TEST_ENGINE_OFF);
    TestBed.tick();
    answerPageLoad([], TEST_ENGINE_OFF);
    await fixture.whenStable();

    expect(element(fixture, ENGINE_OFF).textContent).toContain(ENGINE_OFF_LABEL);
  });

  it('turns the engine back on', async () => {
    const fixture = await render([], TEST_ENGINE_OFF);

    buttonLabelled(fixture, TURN_ON).click();
    const resume = httpMock.expectOne(batchUrl(ENGINE_RESUME_PATH));
    expect(resume.request.method).toBe(POST);
    resume.flush(TEST_ENGINE);
    TestBed.tick();

    answerPageLoad([]);
  });

  it('plans a job of the chosen type for the chosen time', async () => {
    const fixture = await render([]);

    selectOption(fixture, PLAN_TYPE, TEST_TYPE_CODE);
    typeInto(fixture, PLAN_START, PLAN_START_AT);
    submitForm(fixture);
    const plan = httpMock.expectOne(
      (request) => request.url === batchUrl(JOBS_PATH) && request.method === POST,
    );
    expect(plan.request.body).toEqual({
      type: TEST_TYPE_CODE,
      scheduledFor: new Date(PLAN_START_AT).toISOString(),
    });
    plan.flush(RUNNING_JOB);
    TestBed.tick();
    answerPageLoad([RUNNING_JOB]);
    await fixture.whenStable();

    expect(element(fixture, SUCCESS_NOTICE).textContent).toContain(TEST_TYPE.name);
  });

  it('asks what to run before planning anything', async () => {
    const fixture = await render([]);

    submitForm(fixture);

    httpMock.expectNone((request) => request.method === POST);
    expect(elements(fixture, FIELD_ERROR).length).toBeGreaterThan(0);
  });

  it('keeps the table live for a while after a job is started, even with nothing running', async () => {
    vi.useFakeTimers({ toFake: ['setInterval', 'Date'] });
    const fixture = await render([], TEST_ENGINE_IDLE);

    vi.advanceTimersByTime(REFRESH_TICK_MS);
    TestBed.tick();
    httpMock.expectNone(batchUrl(ENGINE_PATH));

    buttonLabelled(fixture, RUN_NOW).click();
    httpMock
      .expectOne((request) => request.url === batchUrl(JOBS_PATH) && request.method === POST)
      .flush(RUNNING_JOB);
    TestBed.tick();
    answerPageLoad([RUNNING_JOB], TEST_ENGINE_IDLE);
    await fixture.whenStable();

    vi.advanceTimersByTime(REFRESH_TICK_MS);
    TestBed.tick();
    answerPageLoad([RUNNING_JOB], TEST_ENGINE_IDLE);
  });

  it('shows why an action was refused', async () => {
    const fixture = await render([FAILED_JOB]);

    buttonLabelled(fixture, ACKNOWLEDGE).click();
    httpMock
      .expectOne(batchUrl(`${JOBS_PATH}/${FAILED_JOB.id}${ACKNOWLEDGE_PATH}`))
      .flush(CONFLICT_BODY, CONFLICT);
    await fixture.whenStable();

    expect(element(fixture, ERROR_NOTICE).textContent).toContain(TEST_CONFLICT_MESSAGE);
  });

  async function render(
    jobs: readonly BatchJobView[],
    engine: BatchEngineView = TEST_ENGINE,
  ): Promise<ComponentFixture<BatchPage>> {
    const fixture = TestBed.createComponent(BatchPage);
    TestBed.tick();
    answerPageLoad(jobs, engine);
    await fixture.whenStable();
    return fixture;
  }

  function answerPageLoad(
    jobs: readonly BatchJobView[],
    engine: BatchEngineView = TEST_ENGINE,
  ): void {
    answerEngineAndTypes(engine);
    jobsRequest().flush(onePage(jobs));
  }

  function answerEngineAndTypes(engine: BatchEngineView = TEST_ENGINE): void {
    httpMock.expectOne(batchUrl(ENGINE_PATH)).flush(engine);
    httpMock.expectOne(batchUrl(TYPES_PATH)).flush([TEST_TYPE]);
  }

  function jobsRequest(): TestRequest {
    return httpMock.expectOne(
      (request) => request.url === batchUrl(JOBS_PATH) && request.method === GET,
    );
  }

  function tab(fixture: ComponentFixture<BatchPage>, which: JobTab): HTMLButtonElement {
    const label = JOB_TABS.find((item) => item.tab === which)?.label ?? which;
    return buttonLabelled(fixture, label);
  }
});
