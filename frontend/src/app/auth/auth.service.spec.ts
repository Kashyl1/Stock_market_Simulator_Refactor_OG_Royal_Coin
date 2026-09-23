import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../environments/environment';
import { Role } from '../core/role';
import { UserStatus } from '../core/user-status';
import { TestUsers } from '../testing/test-users';
import {
  AUTH_PATH,
  Account,
  AuthService,
  LOGIN_PATH,
  LOGOUT_PATH,
  ME_PATH,
  REFRESH_PATH,
  REGISTER_PATH,
  RegisterResponse,
} from './auth.service';

const REGISTER_URL = `${environment.apiUrl}${AUTH_PATH}${REGISTER_PATH}`;
const LOGIN_URL = `${environment.apiUrl}${AUTH_PATH}${LOGIN_PATH}`;
const REFRESH_URL = `${environment.apiUrl}${AUTH_PATH}${REFRESH_PATH}`;
const LOGOUT_URL = `${environment.apiUrl}${AUTH_PATH}${LOGOUT_PATH}`;
const ME_URL = `${environment.apiUrl}${AUTH_PATH}${ME_PATH}`;
const POST_METHOD = 'POST';
const USER_ID = 7;
const ACCESS_TOKEN = 'access-token';
const REFRESHED_TOKEN = 'refreshed-token';
const EXPIRES_IN_SECONDS = 3600;
const SERVER_ERROR = { status: 500, statusText: 'Server Error' };
const CREATED: RegisterResponse = { userId: USER_ID, status: UserStatus.PendingVerification };
const ACCOUNT: Account = {
  id: USER_ID,
  email: TestUsers.email,
  displayName: TestUsers.displayName,
  role: Role.User,
  status: UserStatus.Active,
};

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('posts the registration to the auth endpoint and returns the new account', () => {
    const request = {
      email: TestUsers.email,
      displayName: TestUsers.displayName,
      password: TestUsers.password,
    };
    let response: RegisterResponse | undefined;

    service.register(request).subscribe((result) => (response = result));

    const call = httpMock.expectOne(REGISTER_URL);
    expect(call.request.method).toBe(POST_METHOD);
    expect(call.request.body).toEqual(request);
    call.flush(CREATED);

    expect(response).toEqual(CREATED);
  });

  it('keeps the access token in memory and the account in a signal after a login', () => {
    expect(service.isAuthenticated()).toBe(false);

    service.login({ email: TestUsers.email, password: TestUsers.password }).subscribe();

    const call = httpMock.expectOne(LOGIN_URL);
    expect(call.request.withCredentials).toBe(true);
    call.flush({ accessToken: ACCESS_TOKEN, expiresInSeconds: EXPIRES_IN_SECONDS, user: ACCOUNT });

    expect(service.accessToken()).toBe(ACCESS_TOKEN);
    expect(service.currentUser()).toEqual(ACCOUNT);
    expect(service.isAuthenticated()).toBe(true);
  });

  it('restores a session from the refresh cookie and then loads the account', () => {
    service.restoreSession().subscribe();

    httpMock
      .expectOne(REFRESH_URL)
      .flush({ accessToken: REFRESHED_TOKEN, expiresInSeconds: EXPIRES_IN_SECONDS });
    httpMock.expectOne(ME_URL).flush(ACCOUNT);

    expect(service.accessToken()).toBe(REFRESHED_TOKEN);
    expect(service.currentUser()).toEqual(ACCOUNT);
  });

  it('drops the session on logout even when the call fails', () => {
    givenSignedIn();

    service.logout().subscribe({ error: () => undefined });
    httpMock.expectOne(LOGOUT_URL).flush(null, SERVER_ERROR);

    expect(service.accessToken()).toBeNull();
    expect(service.currentUser()).toBeNull();
    expect(service.isAuthenticated()).toBe(false);
  });

  function givenSignedIn(): void {
    service.login({ email: TestUsers.email, password: TestUsers.password }).subscribe();
    httpMock
      .expectOne(LOGIN_URL)
      .flush({ accessToken: ACCESS_TOKEN, expiresInSeconds: EXPIRES_IN_SECONDS, user: ACCOUNT });
  }
});
