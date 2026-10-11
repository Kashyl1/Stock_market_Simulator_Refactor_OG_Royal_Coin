import { Component, computed, input } from '@angular/core';
import { BatchStatus, STATUS_LABELS } from './batch-models';

@Component({
  selector: 'app-job-status-badge',
  templateUrl: './job-status-badge.html',
  styleUrl: './job-status-badge.scss',
})
export class JobStatusBadge {
  readonly status = input.required<BatchStatus>();

  protected readonly label = computed(() => STATUS_LABELS[this.status()]);
}
