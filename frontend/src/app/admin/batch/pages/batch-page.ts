import { DOCUMENT, DatePipe, formatDate } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, LOCALE_ID, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Observable, interval } from 'rxjs';
import { batchJobPath } from '../../../core/app-routes';
import { toFailure } from '../../../core/error-response';
import { DATE_TIME_FORMAT, LOCAL_DATE_TIME_INPUT_FORMAT } from '../../../core/formats';
import { loadedValue } from '../../../core/loaded-value';
import { FIRST_PAGE } from '../../../core/page-response';
import { BatchAdminService } from '../batch-admin.service';
import {
  BatchJobView,
  ENGINE_OFF_QUESTION,
  JOB_TABS,
  JOB_TAB_FILTERS,
  JobTab,
  LIVE_AFTER_ACTION_MS,
  ORIGIN_LABELS,
  REFRESH_TICK_MS,
  REMOVE_QUESTION,
  STOP_QUESTION,
  canAcknowledge,
  canRemove,
  canStop,
  shouldRefresh,
} from '../batch-models';
import { JobProgress } from '../job-progress';
import { JobStatusBadge } from '../job-status-badge';

const ALL_TYPES = '';
const NO_RUNNING_JOBS = 0;

@Component({
  selector: 'app-batch-page',
  imports: [DatePipe, ReactiveFormsModule, RouterLink, JobProgress, JobStatusBadge],
  templateUrl: './batch-page.html',
  styleUrl: './batch-page.scss',
})
export class BatchPage {
  private readonly batch = inject(BatchAdminService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly document = inject(DOCUMENT);
  private readonly locale = inject(LOCALE_ID);
  private readonly formBuilder = inject(FormBuilder);

  protected readonly tabs = JOB_TABS;
  protected readonly originLabels = ORIGIN_LABELS;
  protected readonly dateFormat = DATE_TIME_FORMAT;
  protected readonly allTypes = ALL_TYPES;
  protected readonly jobPath = batchJobPath;
  protected readonly canStop = canStop;
  protected readonly canRemove = canRemove;
  protected readonly canAcknowledge = canAcknowledge;

  protected readonly tab = signal<JobTab>(JobTab.All);
  protected readonly type = signal<string | null>(null);
  protected readonly page = signal(FIRST_PAGE);
  protected readonly failure = signal<string | null>(null);
  protected readonly planned = signal<BatchJobView | null>(null);

  protected readonly planForm = this.formBuilder.nonNullable.group({
    type: ['', Validators.required],
    startAt: [this.localNow(), Validators.required],
  });

  protected readonly engine = this.batch.engine();
  protected readonly types = this.batch.types();
  protected readonly jobs = this.batch.jobs(() => ({
    ...JOB_TAB_FILTERS[this.tab()],
    type: this.type(),
    page: this.page(),
  }));

  protected readonly engineStatus = loadedValue(this.engine);
  protected readonly typeList = loadedValue(this.types);
  protected readonly jobPage = loadedValue(this.jobs);
  protected readonly enabledTypes = computed(() =>
    (this.typeList() ?? []).filter((type) => type.enabled),
  );

  protected readonly isFirstPage = computed(() => this.page() === FIRST_PAGE);
  protected readonly isLastPage = computed(
    () => this.page() + 1 >= (this.jobPage()?.totalPages ?? 0),
  );
  protected readonly pageNumber = computed(() => this.page() + 1);

  protected readonly typeNames = computed(
    () => new Map((this.typeList() ?? []).map((type) => [type.code, type.name])),
  );

  private readonly liveUntil = signal(0);
  private readonly somethingRuns = computed(
    () => (this.engineStatus()?.runningJobs ?? NO_RUNNING_JOBS) > NO_RUNNING_JOBS,
  );

  constructor() {
    interval(REFRESH_TICK_MS)
      .pipe(takeUntilDestroyed())
      .subscribe((tick) => {
        const live = this.somethingRuns() || Date.now() < this.liveUntil();
        if (shouldRefresh(tick + 1, live, this.document.hidden)) {
          this.reload();
        }
      });
  }

  protected selectTab(tab: JobTab): void {
    this.tab.set(tab);
    this.page.set(FIRST_PAGE);
  }

  protected selectType(event: Event): void {
    const selected = (event.target as HTMLSelectElement).value;
    this.type.set(selected === ALL_TYPES ? null : selected);
    this.page.set(FIRST_PAGE);
  }

  protected nameOf(typeCode: string): string {
    return this.typeNames().get(typeCode) ?? typeCode;
  }

  protected previousPage(): void {
    this.page.update((page) => page - 1);
  }

  protected nextPage(): void {
    this.page.update((page) => page + 1);
  }

  protected turnOff(): void {
    if (confirm(ENGINE_OFF_QUESTION)) {
      this.act(this.batch.pauseEngine());
    }
  }

  protected turnOn(): void {
    this.act(this.batch.resumeEngine());
  }

  protected runNow(type: string): void {
    this.act(this.batch.plan(type));
  }

  protected planJob(): void {
    if (this.planForm.invalid) {
      this.planForm.markAllAsTouched();
      return;
    }
    const { type, startAt } = this.planForm.getRawValue();
    this.planned.set(null);
    this.failure.set(null);
    this.batch
      .plan(type, new Date(startAt).toISOString())
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (job) => {
          this.planned.set(job);
          this.reloadAndStayLive();
        },
        error: (error: HttpErrorResponse) => this.failure.set(toFailure(error).message),
      });
  }

  protected stop(job: BatchJobView): void {
    if (confirm(STOP_QUESTION)) {
      this.act(this.batch.stop(job.id));
    }
  }

  protected remove(job: BatchJobView): void {
    if (confirm(REMOVE_QUESTION)) {
      this.act(this.batch.remove(job.id));
    }
  }

  protected acknowledge(job: BatchJobView): void {
    this.act(this.batch.acknowledge(job.id));
  }

  private act(action: Observable<unknown>): void {
    this.failure.set(null);
    action.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => this.reloadAndStayLive(),
      error: (error: HttpErrorResponse) => this.failure.set(toFailure(error).message),
    });
  }

  private reloadAndStayLive(): void {
    this.liveUntil.set(Date.now() + LIVE_AFTER_ACTION_MS);
    this.reload();
  }

  private reload(): void {
    this.engine.reload();
    this.types.reload();
    this.jobs.reload();
  }

  private localNow(): string {
    return formatDate(Date.now(), LOCAL_DATE_TIME_INPUT_FORMAT, this.locale);
  }
}
