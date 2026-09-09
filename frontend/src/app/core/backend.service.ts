import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface HealthResponse {
  status: string;
  service: string;
  timestamp: string;
}

@Injectable({ providedIn: 'root' })
export class BackendService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = environment.apiUrl;

  health(): Observable<HealthResponse> {
    return this.http.get<HealthResponse>(`${this.baseUrl}/health`);
  }
}
