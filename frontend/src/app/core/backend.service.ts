import { HttpResourceRef, httpResource } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { environment } from '../../environments/environment';

export const HEALTH_PATH = '/health';

export interface HealthResponse {
  readonly status: string;
  readonly service: string;
  readonly timestamp: string;
}

@Injectable({ providedIn: 'root' })
export class BackendService {
  private readonly baseUrl = environment.apiUrl;

  health(): HttpResourceRef<HealthResponse | undefined> {
    return httpResource<HealthResponse>(() => `${this.baseUrl}${HEALTH_PATH}`);
  }
}
