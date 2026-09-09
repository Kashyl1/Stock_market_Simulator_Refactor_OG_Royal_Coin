import { Component, VERSION, inject, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { BackendService, HealthResponse } from './core/backend.service';
import { environment } from '../environments/environment';

type ConnectionStatus = 'checking' | 'online' | 'offline';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App {
  private readonly backend = inject(BackendService);

  protected readonly angularVersion = VERSION.full;
  protected readonly apiUrl = environment.apiUrl;

  protected readonly status = signal<ConnectionStatus>('checking');
  protected readonly health = signal<HealthResponse | null>(null);
  protected readonly error = signal<string | null>(null);

  constructor() {
    this.check();
  }

  protected check(): void {
    this.status.set('checking');
    this.error.set(null);
    this.backend.health().subscribe({
      next: (res) => {
        this.health.set(res);
        this.status.set('online');
      },
      error: (err: unknown) => {
        this.error.set(err instanceof Error ? err.message : 'Request failed');
        this.status.set('offline');
      },
    });
  }
}
