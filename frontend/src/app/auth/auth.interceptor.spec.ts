import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { environment } from '../../environments/environment';
import { AppPath } from '../core/app-routes';
import { accessTokenResponse, authUrl, signIn } from '../testing/auth-session';
import { TEST_ACCESS_TOKEN, TEST_REFRESHED_TOKEN } from '../testing/test-accounts';
import { authInterceptor } from './auth.interceptor';
import { AuthService, REFRESH_PATH } from './auth.service';

const PROTECTED_URL = `${environment.apiUrl}/portfolios`;
const REFRESH_URL = authUrl(REFRESH_PATH);
const AUTHORIZATION_HEADER = 'Authorization';
const UNAUTHORIZED = { status: 401, statusText: 'Unauthorized' };
const PAYLOAD = { ok: true };

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
    signIn(httpMock);
  });

  afterEach(() => httpMock.verify());

  it('sends the access token with a protected call', () => {
    http.get(PROTECTED_URL).subscribe();

    const call = httpMock.expectOne(PROTECTED_URL);
    expect(call.request.headers.get(AUTHORIZATION_HEADER)).toBe(`Bearer ${TEST_ACCESS_TOKEN}`);
    call.flush(PAYLOAD);
  });

  it('refreshes once on a 401 and repeats the call with the new token', () => {
    let body: unknown;
    http.get(PROTECTED_URL).subscribe((result) => (body = result));

    httpMock.expectOne(PROTECTED_URL).flush(null, UNAUTHORIZED);
    httpMock.expectOne(REFRESH_URL).flush(accessTokenResponse(TEST_REFRESHED_TOKEN));

    const retry = httpMock.expectOne(PROTECTED_URL);
    expect(retry.request.headers.get(AUTHORIZATION_HEADER)).toBe(`Bearer ${TEST_REFRESHED_TOKEN}`);
    retry.flush(PAYLOAD);

    expect(body).toEqual(PAYLOAD);
    expect(auth.accessToken()).toBe(TEST_REFRESHED_TOKEN);
  });

  it('ends the session and sends the person to the login page when the refresh fails', () => {
    const navigate = vi.spyOn(router, 'navigate');
    http.get(PROTECTED_URL).subscribe({ error: () => undefined });

    httpMock.expectOne(PROTECTED_URL).flush(null, UNAUTHORIZED);
    httpMock.expectOne(REFRESH_URL).flush(null, UNAUTHORIZED);

    expect(auth.isAuthenticated()).toBe(false);
    expect(navigate).toHaveBeenCalledWith([AppPath.Login]);
  });
});
