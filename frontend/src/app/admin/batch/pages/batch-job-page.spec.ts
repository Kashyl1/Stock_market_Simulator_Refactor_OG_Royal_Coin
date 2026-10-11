import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  TestRequest,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { buttonLabelled, element, elements } from '../../../testing/dom';
import {
  FAILED_ITEM,
  FAILED_JOB,
  SUCCEEDED_ITEM,
  TEST_ERROR_MESSAGE,
  TEST_ITEM_KEY,
  TEST_RATE,
  batchUrl,
  onePage,
} from '../../../testing/test-batch';
import { ACKNOWLEDGE_PATH, BatchParam, ITEMS_PATH, JOBS_PATH } from '../batch-admin.service';
import { BatchItemStatus } from '../batch-models';
import { BatchJobPage } from './batch-job-page';

const HEADING = 'h1';
const ITEM_ROW = '.item';
const PAYLOAD = '.item__payload';
const FAILED = 'Failed';
const ACKNOWLEDGE = 'Acknowledge';
const POST = 'POST';
const JOB_URL = batchUrl(`${JOBS_PATH}/${FAILED_JOB.id}`);
const ITEMS_URL = `${JOB_URL}${ITEMS_PATH}`;

describe('BatchJobPage', () => {
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [BatchJobPage],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('shows the job, its error and every item with its data or its error', async () => {
    const fixture = await render();

    const page = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(element(fixture, HEADING).textContent).toContain(`#${FAILED_JOB.id}`);
    expect(page).toContain(FAILED_JOB.errorMessage);
    expect(elements(fixture, ITEM_ROW).length).toBe(2);
    expect(page).toContain(TEST_ITEM_KEY);
    expect(element(fixture, PAYLOAD).textContent).toContain(String(TEST_RATE));
    expect(page).toContain(TEST_ERROR_MESSAGE);
  });

  it('asks only for the failed items on the Failed filter', async () => {
    const fixture = await render();

    buttonLabelled(fixture, FAILED).click();
    TestBed.tick();
    const failed = itemsRequest();

    expect(failed.request.params.get(BatchParam.Status)).toBe(BatchItemStatus.Failed);
    failed.flush(onePage([FAILED_ITEM]));
  });

  it('acknowledges the failure report and reloads the job', async () => {
    const fixture = await render();

    buttonLabelled(fixture, ACKNOWLEDGE).click();
    const acknowledge = httpMock.expectOne(`${JOB_URL}${ACKNOWLEDGE_PATH}`);
    expect(acknowledge.request.method).toBe(POST);
    acknowledge.flush(FAILED_JOB);
    TestBed.tick();

    answerJobAndItems();
  });

  async function render(): Promise<ComponentFixture<BatchJobPage>> {
    const fixture = TestBed.createComponent(BatchJobPage);
    fixture.componentRef.setInput('id', String(FAILED_JOB.id));
    TestBed.tick();
    answerJobAndItems();
    await fixture.whenStable();
    return fixture;
  }

  function answerJobAndItems(): void {
    httpMock.expectOne(JOB_URL).flush(FAILED_JOB);
    itemsRequest().flush(onePage([SUCCEEDED_ITEM, FAILED_ITEM]));
  }

  function itemsRequest(): TestRequest {
    return httpMock.expectOne((request) => request.url === ITEMS_URL);
  }
});
