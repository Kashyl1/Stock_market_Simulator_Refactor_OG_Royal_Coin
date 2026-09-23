  import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { environment } from '../../environments/environment';
import { AppPath } from '../core/app-routes';
import { Role } from '../core/role';
import { UserStatus } from '../core/user-status';
import { TestUsers } from '../testing/test-users';
import { authInterceptor } from './auth.interceptor';
import { AUTH_PATH, Account, AuthService, LOGIN_PATH, REFRESH_PATH } from './auth.service';

const PROTECTED_URL = `${environment.apiUrl}/portfolios`;
const LOGIN_URL = `${environment.apiUrl}${AUTH_PATH}${LOGIN_PATH}`;
const REFRESH_URL = `${environment.apiUrl}${AUTH_PATH}${REFRESH_PATH}`;
const AUTHORIZATION_HEADER = 'Authorization';
const ACCESS_TOKEN = 'access-token';
const REFRESHED_TOKEN = 'refreshed-token';
const EXPIRES_IN_SECONDS = 3600;
const UNAUTHORIZED = { status: 401, statusText: 'Unauthorized' };
const PAYLOAD = { ok: true };
const ACCOUNT: Account = {
  id: 7,
  email: TestUsers.email,
  displayName: TestUsers.displayName,
  role: Role.User,
  status: UserStatus.Active,
};

describe('authInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;
  let auth: AuthService;
  let router: Router;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        provideRouter([]),
      ],
    });
    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
    auth = TestBed.inject(AuthService);
    router = TestBed.inject(Router);
    signIn();
  });

  afterEach(() => httpMock.verify());

  it('sends the access token with a protected call', () => {
    http.get(PROTECTED_URL).subscribe();

    const call = httpMock.expectOne(PROTECTED_URL);
    expect(call.request.headers.get(AUTHORIZATION_HEADER)).toBe(`Bearer ${ACCESS_TOKEN}`);
    call.flush(PAYLOAD);
  });

  it('refreshes once on a 401 and repeats the call with the new token', () => {
    let body: unknown;
    http.get(PROTECTED_URL).subscribe((result) => (body = result));

    httpMock.expectOne(PROTECTED_URL).flush(null, UNAUTHORIZED);
    httpMock
      .expectOne(REFRESH_URL)
      .flush({ accessToken: REFRESHED_TOKEN, expiresInSeconds: EXPIRES_IN_SECONDS });

    const retry = httpMock.expectOne(PROTECTED_URL);
    expect(retry.request.headers.get(AUTHORIZATION_HEADER)).toBe(`Bearer ${REFRESHED_TOKEN}`);
    retry.flush(PAYLOAD);

    expect(body).toEqual(PAYLOAD);
    expect(auth.accessToken()).toBe(REFRESHED_TOKEN);
  });

  it('ends the session and sends the person to the login page when the refresh fails', () => {
    const navigate = vi.spyOn(router, 'navigate');
    http.get(PROTECTED_URL).subscribe({ error: () => undefined });

    httpMock.expectOne(PROTECTED_URL).flush(null, UNAUTHORIZED);
    httpMock.expectOne(REFRESH_URL).flush(null, UNAUTHORIZED);

    expect(auth.isAuthenticated()).toBe(false);
    expect(navigate).toHaveBeenCalledWith([AppPath.Login]);
  });

  function signIn(): void {
    auth.login({ email: TestUsers.email, password: TestUsers.password }).subscribe();
    httpMock
      .expectOne(LOGIN_URL)
      .flush({ accessToken: ACCESS_TOKEN, expiresInSeconds: EXPIRES_IN_SECONDS, user: ACCOUNT });
  }
});
