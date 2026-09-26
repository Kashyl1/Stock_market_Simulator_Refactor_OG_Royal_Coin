import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, finalize, map, switchMap, tap } from 'rxjs';
import { environment } from '../../environments/environment';
import { Role } from '../core/role';
import { UserStatus } from '../core/user-status';

export const AUTH_PATH = '/auth';
export const REGISTER_PATH = '/register';
export const VERIFY_EMAIL_PATH = '/verify-email';
export const LOGIN_PATH = '/login';
export const REFRESH_PATH = '/refresh';
export const LOGOUT_PATH = '/logout';
export const FORGOT_PASSWORD_PATH = '/forgot-password';
export const RESET_PASSWORD_PATH = '/reset-password';
export const ME_PATH = '/me';

export const PUBLIC_AUTH_PATHS: readonly string[] = [
  REGISTER_PATH,
  VERIFY_EMAIL_PATH,
  LOGIN_PATH,
  REFRESH_PATH,
  LOGOUT_PATH,
  FORGOT_PASSWORD_PATH,
  RESET_PASSWORD_PATH,
];

export interface RegisterRequest {
  readonly email: string;
  readonly displayName: string;
  readonly password: string;
}

export interface RegisterResponse {
  readonly userId: number;
  readonly status: UserStatus;
}

export interface LoginRequest {
  readonly email: string;
  readonly password: string;
}

export interface Account {
  readonly id: number;
  readonly email: string;
  readonly displayName: string;
  readonly role: Role;
  readonly status: UserStatus;
}

interface LoginResponse {
  readonly accessToken: string;
  readonly expiresInSeconds: number;
  readonly user: Account;
}

interface AccessTokenResponse {
  readonly accessToken: string;
  readonly expiresInSeconds: number;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}${AUTH_PATH}`;

  private readonly token = signal<string | null>(null);
  private readonly account = signal<Account | null>(null);

  readonly accessToken = this.token.asReadonly();
  readonly currentUser = this.account.asReadonly();
  readonly isAuthenticated = computed(() => this.token() !== null);

  register(request: RegisterRequest): Observable<RegisterResponse> {
    return this.http.post<RegisterResponse>(`${this.baseUrl}${REGISTER_PATH}`, request);
  }

  verifyEmail(token: string): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}${VERIFY_EMAIL_PATH}`, { token });
  }

  login(request: LoginRequest): Observable<Account> {
    return this.http
      .post<LoginResponse>(`${this.baseUrl}${LOGIN_PATH}`, request, { withCredentials: true })
      .pipe(
        tap((response) => this.token.set(response.accessToken)),
        map((response) => response.user),
        tap((account) => this.account.set(account)),
      );
  }

  requestPasswordReset(email: string): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}${FORGOT_PASSWORD_PATH}`, { email });
  }

  resetPassword(token: string, newPassword: string): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}${RESET_PASSWORD_PATH}`, { token, newPassword });
  }

  refreshAccessToken(): Observable<string> {
    return this.http
      .post<AccessTokenResponse>(`${this.baseUrl}${REFRESH_PATH}`, null, { withCredentials: true })
      .pipe(
        tap((response) => this.token.set(response.accessToken)),
        map((response) => response.accessToken),
      );
  }

  restoreSession(): Observable<Account> {
    return this.refreshAccessToken().pipe(switchMap(() => this.me()));
  }

  me(): Observable<Account> {
    return this.http
      .get<Account>(`${this.baseUrl}${ME_PATH}`)
      .pipe(tap((account) => this.account.set(account)));
  }

  logout(): Observable<void> {
    return this.http
      .post<void>(`${this.baseUrl}${LOGOUT_PATH}`, null, { withCredentials: true })
      .pipe(finalize(() => this.clearSession()));
  }

  clearSession(): void {
    this.token.set(null);
    this.account.set(null);
  }
}
