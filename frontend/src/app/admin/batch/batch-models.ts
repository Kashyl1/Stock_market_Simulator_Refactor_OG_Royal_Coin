export const REFRESH_TICK_MS = 5000;
export const IDLE_REFRESH_TICKS = 6;
export const LIVE_AFTER_ACTION_MS = 60000;
export const STOP_QUESTION = 'Stop this job after the item it is working on?';
export const REMOVE_QUESTION = 'Remove this planned job?';
export const ENGINE_OFF_QUESTION =
  'Turn the batch engine off? No job starts until it is turned back on.';

export const BatchJobStatus = {
  Scheduled: 'SCHEDULED',
  Running: 'RUNNING',
  Completed: 'COMPLETED',
  CompletedWithErrors: 'COMPLETED_WITH_ERRORS',
  Failed: 'FAILED',
  Stopped: 'STOPPED',
} as const;

export type BatchJobStatus = (typeof BatchJobStatus)[keyof typeof BatchJobStatus];

export const BatchJobOrigin = {
  Cron: 'CRON',
  Manual: 'MANUAL',
} as const;

export type BatchJobOrigin = (typeof BatchJobOrigin)[keyof typeof BatchJobOrigin];

export const BatchItemStatus = {
  Succeeded: 'SUCCEEDED',
  Failed: 'FAILED',
} as const;

export type BatchItemStatus = (typeof BatchItemStatus)[keyof typeof BatchItemStatus];

export type BatchStatus = BatchJobStatus | BatchItemStatus;

export const FINISHED_STATUSES: readonly BatchJobStatus[] = [
  BatchJobStatus.Completed,
  BatchJobStatus.CompletedWithErrors,
  BatchJobStatus.Failed,
  BatchJobStatus.Stopped,
];

const REPORT_STATUSES: readonly BatchJobStatus[] = [
  BatchJobStatus.CompletedWithErrors,
  BatchJobStatus.Failed,
];

export interface BatchEngineView {
  readonly on: boolean;
  readonly switchedBy: string;
  readonly switchedAt: string;
  readonly quiet: boolean;
  readonly quietFrom: string | null;
  readonly quietUntil: string | null;
  readonly zone: string | null;
  readonly runningJobs: number;
  readonly readyForDeploy: boolean;
}

export interface BatchTypeView {
  readonly code: string;
  readonly name: string;
  readonly description: string;
  readonly cron: string | null;
  readonly zone: string;
  readonly enabled: boolean;
  readonly nextRunAt: string | null;
  readonly lastStatus: BatchJobStatus | null;
  readonly lastFinishedAt: string | null;
}

export interface BatchJobView {
  readonly id: number;
  readonly type: string;
  readonly status: BatchJobStatus;
  readonly origin: BatchJobOrigin;
  readonly scheduledFor: string;
  readonly plannedBy: string;
  readonly startedAt: string | null;
  readonly finishedAt: string | null;
  readonly totalItems: number | null;
  readonly succeededItems: number;
  readonly failedItems: number;
  readonly errorCode: string | null;
  readonly errorMessage: string | null;
  readonly stopRequestedBy: string | null;
  readonly stopRequestedAt: string | null;
  readonly acknowledgedBy: string | null;
  readonly acknowledgedAt: string | null;
}

export interface BatchItemView {
  readonly id: number;
  readonly key: string;
  readonly status: BatchItemStatus;
  readonly payload: unknown;
  readonly errorCode: string | null;
  readonly errorMessage: string | null;
  readonly processedAt: string;
}

export const JobTab = {
  All: 'all',
  Running: 'running',
  Scheduled: 'scheduled',
  Finished: 'finished',
  ToReview: 'to-review',
} as const;

export type JobTab = (typeof JobTab)[keyof typeof JobTab];

export interface JobFilter {
  readonly statuses: readonly BatchJobStatus[];
  readonly unacknowledged: boolean;
}

export const JOB_TAB_FILTERS: Readonly<Record<JobTab, JobFilter>> = {
  [JobTab.All]: { statuses: [], unacknowledged: false },
  [JobTab.Running]: { statuses: [BatchJobStatus.Running], unacknowledged: false },
  [JobTab.Scheduled]: { statuses: [BatchJobStatus.Scheduled], unacknowledged: false },
  [JobTab.Finished]: { statuses: FINISHED_STATUSES, unacknowledged: false },
  [JobTab.ToReview]: { statuses: [], unacknowledged: true },
};

export const JOB_TABS: readonly { readonly tab: JobTab; readonly label: string }[] = [
  { tab: JobTab.All, label: 'All' },
  { tab: JobTab.Running, label: 'Running' },
  { tab: JobTab.Scheduled, label: 'Scheduled' },
  { tab: JobTab.Finished, label: 'Finished' },
  { tab: JobTab.ToReview, label: 'To review' },
];

export const STATUS_LABELS: Readonly<Record<BatchStatus, string>> = {
  [BatchJobStatus.Scheduled]: 'Scheduled',
  [BatchJobStatus.Running]: 'Running',
  [BatchJobStatus.Completed]: 'Completed',
  [BatchJobStatus.CompletedWithErrors]: 'With errors',
  [BatchJobStatus.Failed]: 'Failed',
  [BatchJobStatus.Stopped]: 'Stopped',
  [BatchItemStatus.Succeeded]: 'Succeeded',
};

export const ORIGIN_LABELS: Readonly<Record<BatchJobOrigin, string>> = {
  [BatchJobOrigin.Cron]: 'Cron',
  [BatchJobOrigin.Manual]: 'Manual',
};

export function shouldRefresh(tick: number, somethingRuns: boolean, pageHidden: boolean): boolean {
  if (pageHidden) {
    return false;
  }
  return somethingRuns || tick % IDLE_REFRESH_TICKS === 0;
}

export function canStop(job: BatchJobView): boolean {
  return job.status === BatchJobStatus.Running;
}

export function canRemove(job: BatchJobView): boolean {
  return job.status === BatchJobStatus.Scheduled;
}

export function canAcknowledge(job: BatchJobView): boolean {
  return REPORT_STATUSES.includes(job.status) && job.acknowledgedAt === null;
}
