import { HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../environments/environment';
import { AUTH_PATH, AuthService, LOGIN_PATH } from '../auth/auth.service';
import { TEST_ACCESS_TOKEN, TEST_ACCOUNT, TEST_EXPIRES_IN_SECONDS } from './test-accounts';
import { TestUsers } from './test-users';

export function authUrl(path: string): string {
  return `${environment.apiUrl}${AUTH_PATH}${path}`;
}

export function loginResponse() {
  return {
    accessToken: TEST_ACCESS_TOKEN,
    expiresInSeconds: TEST_EXPIRES_IN_SECONDS,
    user: TEST_ACCOUNT,
  };
}

export function accessTokenResponse(accessToken: string) {
  return { accessToken, expiresInSeconds: TEST_EXPIRES_IN_SECONDS };
}

export function signIn(httpMock: HttpTestingController): void {
  TestBed.inject(AuthService)
    .login({ email: TestUsers.email, password: TestUsers.password })
    .subscribe();
  httpMock.expectOne(authUrl(LOGIN_PATH)).flush(loginResponse());
}
