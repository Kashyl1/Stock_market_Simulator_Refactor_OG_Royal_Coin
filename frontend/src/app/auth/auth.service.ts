import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { UserStatus } from '../core/user-status';

export const AUTH_PATH = '/auth';
export const REGISTER_PATH = '/register';

export interface RegisterRequest {
  readonly email: string;
  readonly displayName: string;
  readonly password: string;
}

export interface RegisterResponse {
  readonly userId: number;
  readonly status: UserStatus;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}${AUTH_PATH}`;

  register(request: RegisterRequest): Observable<RegisterResponse> {
    return this.http.post<RegisterResponse>(`${this.baseUrl}${REGISTER_PATH}`, request);
  }
}
