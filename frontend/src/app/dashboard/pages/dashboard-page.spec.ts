import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../../environments/environment';
import { Role } from '../../core/role';
import { UserStatus } from '../../core/user-status';
import { element } from '../../testing/dom';
import { TestUsers } from '../../testing/test-users';
import { AUTH_PATH, Account, AuthService, LOGIN_PATH } from '../../auth/auth.service';
import { DashboardPage } from './dashboard-page';

const LOGIN_URL = `${environment.apiUrl}${AUTH_PATH}${LOGIN_PATH}`;
const HEADING = 'h1';
const ACCESS_TOKEN = 'access-token';
const EXPIRES_IN_SECONDS = 3600;
const ACCOUNT: Account = {
  id: 7,
  email: TestUsers.email,
  displayName: TestUsers.displayName,
  role: Role.User,
  status: UserStatus.Active,
};

describe('DashboardPage', () => {
  let httpMock: HttpTestingController;
  let auth: AuthService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DashboardPage],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    httpMock = TestBed.inject(HttpTestingController);
    auth = TestBed.inject(AuthService);
  });

  afterEach(() => httpMock.verify());

  it('greets the signed in person by their display name and shows the account', () => {
    auth.login({ email: TestUsers.email, password: TestUsers.password }).subscribe();
    httpMock
      .expectOne(LOGIN_URL)
      .flush({ accessToken: ACCESS_TOKEN, expiresInSeconds: EXPIRES_IN_SECONDS, user: ACCOUNT });

    const fixture = TestBed.createComponent(DashboardPage);
    TestBed.tick();

    expect(element(fixture, HEADING).textContent).toContain(TestUsers.displayName);
    expect((fixture.nativeElement as HTMLElement).textContent).toContain(TestUsers.email);
  });
});
