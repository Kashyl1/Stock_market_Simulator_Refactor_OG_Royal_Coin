import { HttpClient, HttpResourceRef, httpResource } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { PageResponse } from '../../core/page-response';
import {
  BatchEngineView,
  BatchItemStatus,
  BatchItemView,
  BatchJobView,
  BatchTypeView,
  JobFilter,
} from './batch-models';

export const ADMIN_BATCH_PATH = '/admin/batch';
export const ENGINE_PATH = '/engine';
export const ENGINE_PAUSE_PATH = `${ENGINE_PATH}/pause`;
export const ENGINE_RESUME_PATH = `${ENGINE_PATH}/resume`;
export const TYPES_PATH = '/types';
export const JOBS_PATH = '/jobs';
export const ITEMS_PATH = '/items';
export const STOP_PATH = '/stop';
export const ACKNOWLEDGE_PATH = '/acknowledge';
export const BATCH_PAGE_SIZE = 20;

export const BatchParam = {
  Status: 'status',
  Type: 'type',
  Unacknowledged: 'unacknowledged',
  Page: 'page',
  Size: 'size',
} as const;

export interface JobsQuery extends JobFilter {
  readonly type: string | null;
  readonly page: number;
}

export interface ItemsQuery {
  readonly jobId: number;
  readonly status: BatchItemStatus | null;
  readonly page: number;
}

type QueryParams = Record<string, string | number | boolean | readonly string[]>;

@Injectable({ providedIn: 'root' })
export class BatchAdminService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}${ADMIN_BATCH_PATH}`;

  engine(): HttpResourceRef<BatchEngineView | undefined> {
    return httpResource<BatchEngineView>(() => `${this.baseUrl}${ENGINE_PATH}`);
  }

  types(): HttpResourceRef<readonly BatchTypeView[] | undefined> {
    return httpResource<readonly BatchTypeView[]>(() => `${this.baseUrl}${TYPES_PATH}`);
  }

  jobs(query: () => JobsQuery): HttpResourceRef<PageResponse<BatchJobView> | undefined> {
    return httpResource<PageResponse<BatchJobView>>(() => ({
      url: `${this.baseUrl}${JOBS_PATH}`,
      params: jobsParams(query()),
    }));
  }

  job(jobId: () => number): HttpResourceRef<BatchJobView | undefined> {
    return httpResource<BatchJobView>(() => this.jobUrl(jobId()));
  }

  items(query: () => ItemsQuery): HttpResourceRef<PageResponse<BatchItemView> | undefined> {
    return httpResource<PageResponse<BatchItemView>>(() => {
      const { jobId, status, page } = query();
      return { url: `${this.jobUrl(jobId)}${ITEMS_PATH}`, params: itemsParams(status, page) };
    });
  }

  plan(type: string, scheduledFor: string | null = null): Observable<BatchJobView> {
    return this.http.post<BatchJobView>(`${this.baseUrl}${JOBS_PATH}`, { type, scheduledFor });
  }

  pauseEngine(): Observable<BatchEngineView> {
    return this.http.post<BatchEngineView>(`${this.baseUrl}${ENGINE_PAUSE_PATH}`, null);
  }

  resumeEngine(): Observable<BatchEngineView> {
    return this.http.post<BatchEngineView>(`${this.baseUrl}${ENGINE_RESUME_PATH}`, null);
  }

  remove(jobId: number): Observable<void> {
    return this.http.delete<void>(this.jobUrl(jobId));
  }

  stop(jobId: number): Observable<void> {
    return this.http.post<void>(`${this.jobUrl(jobId)}${STOP_PATH}`, null);
  }

  acknowledge(jobId: number): Observable<BatchJobView> {
    return this.http.post<BatchJobView>(`${this.jobUrl(jobId)}${ACKNOWLEDGE_PATH}`, null);
  }

  private jobUrl(jobId: number): string {
    return `${this.baseUrl}${JOBS_PATH}/${jobId}`;
  }
}

function jobsParams(query: JobsQuery): QueryParams {
  const params: QueryParams = { [BatchParam.Page]: query.page, [BatchParam.Size]: BATCH_PAGE_SIZE };
  if (query.statuses.length > 0) {
    params[BatchParam.Status] = query.statuses;
  }
  if (query.type !== null) {
    params[BatchParam.Type] = query.type;
  }
  if (query.unacknowledged) {
    params[BatchParam.Unacknowledged] = true;
  }
  return params;
}

function itemsParams(status: BatchItemStatus | null, page: number): QueryParams {
  const params: QueryParams = { [BatchParam.Page]: page, [BatchParam.Size]: BATCH_PAGE_SIZE };
  if (status !== null) {
    params[BatchParam.Status] = status;
  }
  return params;
}
