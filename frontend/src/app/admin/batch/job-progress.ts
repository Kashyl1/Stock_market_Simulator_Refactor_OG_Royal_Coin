import { Component, computed, input } from '@angular/core';

const FULL = 100;

@Component({
  selector: 'app-job-progress',
  templateUrl: './job-progress.html',
  styleUrl: './job-progress.scss',
})
export class JobProgress {
  readonly succeeded = input.required<number>();
  readonly failed = input.required<number>();
  readonly total = input<number | null>(null);

  protected readonly knownTotal = computed(() => {
    const total = this.total();
    return total !== null && total > 0 ? total : null;
  });
  protected readonly succeededShare = computed(() => share(this.succeeded(), this.knownTotal()));
  protected readonly failedShare = computed(() => share(this.failed(), this.knownTotal()));
}

function share(count: number, total: number | null): number {
  return total === null ? 0 : Math.min(FULL, (count / total) * FULL);
}
