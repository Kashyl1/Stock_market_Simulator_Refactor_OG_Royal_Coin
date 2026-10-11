import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import {
  FAILED_JOB,
  RUNNING_JOB,
  TEST_ENGINE,
  TEST_ENGINE_OFF,
  TEST_TYPE_CODE,
  batchUrl,
} from '../../testing/test-batch';
import {
  ACKNOWLEDGE_PATH,
  BatchAdminService,
  ENGINE_PAUSE_PATH,
  ENGINE_RESUME_PATH,
  JOBS_PATH,
  STOP_PATH,
} from './batch-admin.service';

const POST = 'POST';
const DELETE = 'DELETE';

describe('BatchAdminService', () => {
  let service: BatchAdminService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(BatchAdminService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('plans a job of a type', () => {
    service.plan(TEST_TYPE_CODE).subscribe();

    const request = httpMock.expectOne(batchUrl(JOBS_PATH));
    expect(request.request.method).toBe(POST);
    expect(request.request.body).toEqual({ type: TEST_TYPE_CODE, scheduledFor: null });
    request.flush(RUNNING_JOB);
  });

  it('pauses and resumes the engine', () => {
    service.pauseEngine().subscribe();
    const pause = httpMock.expectOne(batchUrl(ENGINE_PAUSE_PATH));
    expect(pause.request.method).toBe(POST);
    pause.flush(TEST_ENGINE_OFF);

    service.resumeEngine().subscribe();
    const resume = httpMock.expectOne(batchUrl(ENGINE_RESUME_PATH));
    expect(resume.request.method).toBe(POST);
    resume.flush(TEST_ENGINE);
  });

  it('removes a planned job', () => {
    service.remove(RUNNING_JOB.id).subscribe();

    const request = httpMock.expectOne(batchUrl(`${JOBS_PATH}/${RUNNING_JOB.id}`));
    expect(request.request.method).toBe(DELETE);
    request.flush(null);
  });

  it('stops a running job', () => {
    service.stop(RUNNING_JOB.id).subscribe();

    const request = httpMock.expectOne(batchUrl(`${JOBS_PATH}/${RUNNING_JOB.id}${STOP_PATH}`));
    expect(request.request.method).toBe(POST);
    request.flush(null);
  });

  it('acknowledges a failure report', () => {
    service.acknowledge(FAILED_JOB.id).subscribe();

    const request = httpMock.expectOne(
      batchUrl(`${JOBS_PATH}/${FAILED_JOB.id}${ACKNOWLEDGE_PATH}`),
    );
    expect(request.request.method).toBe(POST);
    request.flush(FAILED_JOB);
  });
});
