import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import {
  ActivatedRouteSnapshot,
  RouterStateSnapshot,
  UrlTree,
  provideRouter,
} from '@angular/router';
import { environment } from '../../environments/environment';
import { AppPath } from '../core/app-routes';
import { Role } from '../core/role';
import { UserStatus } from '../core/user-status';
import { TestUsers } from '../testing/test-users';
import { REDIRECT_PARAM, authGuard } from './auth.guard';
import { AUTH_PATH, Account, AuthService, LOGIN_PATH } from './auth.service';

const LOGIN_URL = `${environment.apiUrl}${AUTH_PATH}${LOGIN_PATH}`;
const ACCESS_TOKEN = 'access-token';
const EXPIRES_IN_SECONDS = 3600;
const GUARDED_URL = AppPath.Dashboard;
const ROUTE = {} as ActivatedRouteSnapshot;
const STATE = { url: GUARDED_URL } as RouterStateSnapshot;
const ACCOUNT: Account = {
  id: 7,
  email: TestUsers.email,
  displayName: TestUsers.displayName,
  role: Role.User,
  status: UserStatus.Active,
};

describe('authGuard', () => {
  let httpMock: HttpTestingController;
  let auth: AuthService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    httpMock = TestBed.inject(HttpTestingController);
    auth = TestBed.inject(AuthService);
  });

  afterEach(() => httpMock.verify());

  it('sends a signed out visitor to the login page and remembers where they wanted to go', () => {
    const result = runGuard();

    expect(result).toBeInstanceOf(UrlTree);
    expect(String(result)).toContain(AppPath.Login);
    expect(String(result)).toContain(`${REDIRECT_PARAM}=`);
  });

  it('lets a signed in person through', () => {
    auth.login({ email: TestUsers.email, password: TestUsers.password }).subscribe();
    httpMock
      .expectOne(LOGIN_URL)
      .flush({ accessToken: ACCESS_TOKEN, expiresInSeconds: EXPIRES_IN_SECONDS, user: ACCOUNT });

    expect(runGuard()).toBe(true);
  });

  function runGuard(): boolean | UrlTree {
    return TestBed.runInInjectionContext(() => authGuard(ROUTE, STATE)) as boolean | UrlTree;
  }
});
