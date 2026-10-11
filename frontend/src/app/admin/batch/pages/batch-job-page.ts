import { DOCUMENT, DatePipe, JsonPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, computed, inject, input, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Router, RouterLink } from '@angular/router';
import { Observable, interval } from 'rxjs';
import { AppPath } from '../../../core/app-routes';
import { toFailure } from '../../../core/error-response';
import { DATE_TIME_FORMAT } from '../../../core/formats';
import { loadedValue } from '../../../core/loaded-value';
import { FIRST_PAGE } from '../../../core/page-response';
import { BatchAdminService } from '../batch-admin.service';
import {
  BatchItemStatus,
  BatchJobStatus,
  BatchJobView,
  ORIGIN_LABELS,
  REFRESH_TICK_MS,
  REMOVE_QUESTION,
  STOP_QUESTION,
  canAcknowledge,
  canRemove,
  canStop,
} from '../batch-models';
import { JobProgress } from '../job-progress';
import { JobStatusBadge } from '../job-status-badge';

@Component({
  selector: 'app-batch-job-page',
  imports: [DatePipe, JsonPipe, RouterLink, JobProgress, JobStatusBadge],
  templateUrl: './batch-job-page.html',
  styleUrl: './batch-job-page.scss',
})
export class BatchJobPage {
  readonly id = input.required<string>();

  private readonly batch = inject(BatchAdminService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  private readonly document = inject(DOCUMENT);

  protected readonly paths = AppPath;
  protected readonly originLabels = ORIGIN_LABELS;
  protected readonly dateFormat = DATE_TIME_FORMAT;
  protected readonly failedStatus = BatchItemStatus.Failed;
  protected readonly canStop = canStop;
  protected readonly canRemove = canRemove;
  protected readonly canAcknowledge = canAcknowledge;

  protected readonly itemStatus = signal<BatchItemStatus | null>(null);
  protected readonly page = signal(FIRST_PAGE);
  protected readonly failure = signal<string | null>(null);

  private readonly jobId = computed(() => Number(this.id()));

  protected readonly job = this.batch.job(this.jobId);
  protected readonly items = this.batch.items(() => ({
    jobId: this.jobId(),
    status: this.itemStatus(),
    page: this.page(),
  }));

  protected readonly jobView = loadedValue(this.job);
  protected readonly itemPage = loadedValue(this.items);

  protected readonly isFirstPage = computed(() => this.page() === FIRST_PAGE);
  protected readonly isLastPage = computed(
    () => this.page() + 1 >= (this.itemPage()?.totalPages ?? 0),
  );
  protected readonly pageNumber = computed(() => this.page() + 1);

  constructor() {
    interval(REFRESH_TICK_MS)
      .pipe(takeUntilDestroyed())
      .subscribe(() => {
        if (this.jobView()?.status === BatchJobStatus.Running && !this.document.hidden) {
          this.reload();
        }
      });
  }

  protected showItems(status: BatchItemStatus | null): void {
    this.itemStatus.set(status);
    this.page.set(FIRST_PAGE);
  }

  protected previousPage(): void {
    this.page.update((page) => page - 1);
  }

  protected nextPage(): void {
    this.page.update((page) => page + 1);
  }

  protected stop(job: BatchJobView): void {
    if (confirm(STOP_QUESTION)) {
      this.act(this.batch.stop(job.id), () => this.reload());
    }
  }

  protected remove(job: BatchJobView): void {
    if (confirm(REMOVE_QUESTION)) {
      this.act(this.batch.remove(job.id), () => this.router.navigate([AppPath.AdminBatch]));
    }
  }

  protected acknowledge(job: BatchJobView): void {
    this.act(this.batch.acknowledge(job.id), () => this.reload());
  }

  private act(action: Observable<unknown>, then: () => void): void {
    this.failure.set(null);
    action.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => then(),
      error: (error: HttpErrorResponse) => this.failure.set(toFailure(error).message),
    });
  }

  private reload(): void {
    this.job.reload();
    this.items.reload();
  }
}
