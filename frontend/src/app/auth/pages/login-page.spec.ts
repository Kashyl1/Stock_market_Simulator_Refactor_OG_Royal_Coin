import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { AppPath } from '../../core/app-routes';
import { ErrorResponse } from '../../core/error-response';
import { authUrl, loginResponse } from '../../testing/auth-session';
import { element, elements, submitForm, typeInto } from '../../testing/dom';
import { TestUsers } from '../../testing/test-users';
import { LOGIN_PATH } from '../auth.service';
import { LoginPage } from './login-page';

const LOGIN_URL = authUrl(LOGIN_PATH);
const EMAIL_INPUT = '#email';
const PASSWORD_INPUT = '#password';
const FIELD_ERROR = '.field__error';
const ERROR_NOTICE = '.notice--error';
const UNAUTHORIZED = { status: 401, statusText: 'Unauthorized' };
const WRONG_CREDENTIALS: ErrorResponse = {
  code: 'AUTH.INVALID_CREDENTIALS',
  message: 'the e-mail or the password is not correct',
  status: UNAUTHORIZED.status,
  timestamp: '2026-09-23T10:00:00Z',
  path: LOGIN_URL,
  fieldErrors: [],
};

describe('LoginPage', () => {
  let httpMock: HttpTestingController;
  let router: Router;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LoginPage],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    httpMock = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
  });

  afterEach(() => httpMock.verify());

  it('asks for the missing credentials before calling the backend', () => {
    const fixture = render();

    submitForm(fixture);

    httpMock.expectNone(LOGIN_URL);
    expect(elements(fixture, FIELD_ERROR).length).toBeGreaterThan(0);
  });

  it('signs in and moves on to the dashboard', () => {
    const navigate = vi.spyOn(router, 'navigateByUrl');
    const fixture = render();
    fill(fixture);

    submitForm(fixture);

    const call = httpMock.expectOne(LOGIN_URL);
    expect(call.request.body).toEqual({ email: TestUsers.email, password: TestUsers.password });
    call.flush(loginResponse());
    TestBed.tick();

    expect(navigate).toHaveBeenCalledWith(AppPath.Dashboard);
  });

  it('shows the backend message when the credentials are wrong', () => {
    const fixture = render();
    fill(fixture);

    submitForm(fixture);
    httpMock.expectOne(LOGIN_URL).flush(WRONG_CREDENTIALS, UNAUTHORIZED);
    TestBed.tick();

    expect(element(fixture, ERROR_NOTICE).textContent).toContain(WRONG_CREDENTIALS.message);
  });
});

function render(): ComponentFixture<LoginPage> {
  const fixture = TestBed.createComponent(LoginPage);
  TestBed.tick();
  return fixture;
}

function fill(fixture: ComponentFixture<LoginPage>): void {
  typeInto(fixture, EMAIL_INPUT, TestUsers.email);
  typeInto(fixture, PASSWORD_INPUT, TestUsers.password);
}
