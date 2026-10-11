import { environment } from '../../environments/environment';
import { ADMIN_BATCH_PATH } from '../admin/batch/batch-admin.service';
import {
  BatchEngineView,
  BatchItemStatus,
  BatchItemView,
  BatchJobOrigin,
  BatchJobStatus,
  BatchJobView,
  BatchTypeView,
} from '../admin/batch/batch-models';
import { FIRST_PAGE, PageResponse } from '../core/page-response';

const ZONE = 'Europe/Warsaw';
const SINGLE = 1;
const NONE = 0;
const SYSTEM = 'system';

export const TEST_TYPE_CODE = 'FX_RATES_NBP';
export const TEST_ADMIN = 'user:1';
export const TEST_TYPE_NAME = 'FX rates from NBP';
export const TEST_ITEM_KEY = 'USD 2026-10-08';
export const TEST_RATE = 3.9132;
export const TEST_ERROR_CODE = 'FX.RATE_MISSING_FROM_SOURCE';
export const TEST_ERROR_MESSAGE = 'NBP published no rate for EUR on 2026-10-08';
export const TEST_CONFLICT_MESSAGE = 'batch job 1 is COMPLETED; only a running job can be stopped';

export const TEST_ENGINE: BatchEngineView = {
  on: true,
  switchedBy: SYSTEM,
  switchedAt: '2026-10-10T09:00:00Z',
  quiet: false,
  quietFrom: '00:00:00',
  quietUntil: '02:30:00',
  zone: ZONE,
  runningJobs: SINGLE,
  readyForDeploy: false,
};

export const TEST_ENGINE_IDLE: BatchEngineView = { ...TEST_ENGINE, runningJobs: NONE };

export const TEST_ENGINE_OFF: BatchEngineView = {
  ...TEST_ENGINE,
  on: false,
  switchedBy: TEST_ADMIN,
};

export const TEST_TYPE: BatchTypeView = {
  code: TEST_TYPE_CODE,
  name: TEST_TYPE_NAME,
  description: 'Stores the NBP table A mid rates',
  cron: '0 30 12,16 * * MON-FRI',
  zone: ZONE,
  enabled: true,
  nextRunAt: '2026-10-12T10:30:00Z',
  lastStatus: BatchJobStatus.Completed,
  lastFinishedAt: '2026-10-09T14:31:00Z',
};

const JOB: BatchJobView = {
  id: SINGLE,
  type: TEST_TYPE_CODE,
  status: BatchJobStatus.Running,
  origin: BatchJobOrigin.Cron,
  scheduledFor: '2026-10-10T10:30:00Z',
  plannedBy: SYSTEM,
  startedAt: '2026-10-10T10:30:05Z',
  finishedAt: null,
  totalItems: 29,
  succeededItems: 12,
  failedItems: 1,
  errorCode: null,
  errorMessage: null,
  stopRequestedBy: null,
  stopRequestedAt: null,
  acknowledgedBy: null,
  acknowledgedAt: null,
};

export const RUNNING_JOB: BatchJobView = JOB;

export const FAILED_JOB: BatchJobView = {
  ...JOB,
  id: 2,
  status: BatchJobStatus.Failed,
  finishedAt: '2026-10-10T10:31:00Z',
  errorCode: 'BATCH.JOB_INTERRUPTED',
  errorMessage: 'the application stopped while the job was running',
};

export const SUCCEEDED_ITEM: BatchItemView = {
  id: 10,
  key: TEST_ITEM_KEY,
  status: BatchItemStatus.Succeeded,
  payload: { baseCurrency: 'USD', quoteCurrency: 'PLN', rate: TEST_RATE, outcome: 'INSERTED' },
  errorCode: null,
  errorMessage: null,
  processedAt: '2026-10-10T10:30:06Z',
};

export const FAILED_ITEM: BatchItemView = {
  ...SUCCEEDED_ITEM,
  id: 11,
  key: 'EUR 2026-10-08',
  status: BatchItemStatus.Failed,
  payload: null,
  errorCode: TEST_ERROR_CODE,
  errorMessage: TEST_ERROR_MESSAGE,
};

export function onePage<T>(content: readonly T[]): PageResponse<T> {
  return {
    content,
    page: FIRST_PAGE,
    size: content.length,
    totalElements: content.length,
    totalPages: SINGLE,
  };
}

export function batchUrl(path: string): string {
  return `${environment.apiUrl}${ADMIN_BATCH_PATH}${path}`;
}
