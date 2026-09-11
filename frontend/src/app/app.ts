import { Component, VERSION, computed, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { BackendService, HEALTH_PATH } from './core/backend.service';
import { environment } from '../environments/environment';

export const ConnectionStatus = {
  Checking: 'checking',
  Online: 'online',
  Offline: 'offline',
} as const;

export type ConnectionStatus = (typeof ConnectionStatus)[keyof typeof ConnectionStatus];

@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App {
  private readonly backend = inject(BackendService);

  protected readonly angularVersion = VERSION.full;
  protected readonly healthUrl = `${environment.apiUrl}${HEALTH_PATH}`;
  protected readonly statuses = ConnectionStatus;

  protected readonly health = this.backend.health();

  protected readonly status = computed<ConnectionStatus>(() => {
    switch (this.health.status()) {
      case 'resolved':
      case 'local':
        return ConnectionStatus.Online;
      case 'error':
        return ConnectionStatus.Offline;
      default:
        return ConnectionStatus.Checking;
    }
  });

  protected readonly errorMessage = computed(() => this.health.error()?.message ?? null);

  protected check(): void {
    this.health.reload();
  }
}
