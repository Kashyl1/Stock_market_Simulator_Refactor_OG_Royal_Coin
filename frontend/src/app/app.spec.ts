import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { environment } from '../environments/environment';
import { App } from './app';
import { AUTH_PATH, Account, AuthService, LOGIN_PATH } from './auth/auth.service';
import { APP_DISCLAIMER, APP_NAME } from './core/app-info';
import { AppPath } from './core/app-routes';
import { Role } from './core/role';
import { UserStatus } from './core/user-status';
import { attributes, element, elements, texts } from './testing/dom';
import { TestUsers } from './testing/test-users';

const LOGIN_URL = `${environment.apiUrl}${AUTH_PATH}${LOGIN_PATH}`;
const BRAND = '.brand';
const NAV_LINK = '.top-bar__nav a';
const NAV_BUTTON = '.top-bar__nav button';
const OUTLET = 'router-outlet';
const HREF = 'href';
const ACCESS_TOKEN = 'access-token';
const EXPIRES_IN_SECONDS = 3600;
const LOG_OUT_LABEL = 'Log out';
const ACCOUNT: Account = {
  id: 7,
  email: TestUsers.email,
  displayName: TestUsers.displayName,
  role: Role.User,
  status: UserStatus.Active,
};

describe('App', () => {
  let httpMock: HttpTestingController;
  let auth: AuthService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    httpMock = TestBed.inject(HttpTestingController);
    auth = TestBed.inject(AuthService);
  });

  afterEach(() => httpMock.verify());

  it('shows the product name and keeps a place for the routed page', () => {
    const fixture = TestBed.createComponent(App);
    TestBed.tick();

    expect(element(fixture, BRAND).textContent).toContain(APP_NAME);
    expect(elements(fixture, OUTLET).length).toBe(1);
    expect((fixture.nativeElement as HTMLElement).textContent).toContain(APP_DISCLAIMER);
  });

  it('links to log in and to registration while nobody is signed in', () => {
    const fixture = TestBed.createComponent(App);
    TestBed.tick();

    expect(attributes(fixture, NAV_LINK, HREF)).toEqual([AppPath.Login, AppPath.Register]);
  });

  it('swaps the header for the display name and a log out button once signed in', () => {
    signIn();

    const fixture = TestBed.createComponent(App);
    TestBed.tick();

    expect(attributes(fixture, NAV_LINK, HREF)).toEqual([AppPath.Dashboard]);
    expect(texts(fixture, NAV_LINK)).toEqual([TestUsers.displayName]);
    expect(element(fixture, NAV_BUTTON).textContent).toContain(LOG_OUT_LABEL);
  });

  function signIn(): void {
    auth.login({ email: TestUsers.email, password: TestUsers.password }).subscribe();
    httpMock
      .expectOne(LOGIN_URL)
      .flush({ accessToken: ACCESS_TOKEN, expiresInSeconds: EXPIRES_IN_SECONDS, user: ACCOUNT });
  }
});
