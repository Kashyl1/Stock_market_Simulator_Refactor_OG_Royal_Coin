import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { AppPath } from '../../core/app-routes';
import { ErrorResponse } from '../../core/error-response';
import { authUrl, loginResponse } from '../../testing/auth-session';
import { buttonLabelled, element, elements, submitForm, typeInto } from '../../testing/dom';
import { TestUsers } from '../../testing/test-users';
import { AuthErrorCode } from '../auth-errors';
import { LOGIN_PATH, RESEND_VERIFICATION_PATH } from '../auth.service';
import { LoginPage } from './login-page';

const LOGIN_URL = authUrl(LOGIN_PATH);
const EMAIL_INPUT = '#email';
const PASSWORD_INPUT = '#password';
const FIELD_ERROR = '.field__error';
const ERROR_NOTICE = '.notice--error';
const UNAUTHORIZED = { status: 401, statusText: 'Unauthorized' };
const FORBIDDEN = { status: 403, statusText: 'Forbidden' };
const TOO_MANY = { status: 429, statusText: 'Too Many Requests' };
const RESEND_URL = authUrl(RESEND_VERIFICATION_PATH);
const RESEND_BUTTON = 'Send the verification link again';
const SUCCESS_NOTICE = '.notice--success';
const RESEND = '.resend';
const NOTHING = 0;
const RESEND_SENT = 'on its way';
const WRONG_CREDENTIALS: ErrorResponse = {
  code: 'AUTH.INVALID_CREDENTIALS',
  message: 'the e-mail or the password is not correct',
  status: UNAUTHORIZED.status,
  timestamp: '2026-09-23T10:00:00Z',
  path: LOGIN_URL,
  fieldErrors: [],
};

const NOT_VERIFIED: ErrorResponse = {
  ...WRONG_CREDENTIALS,
  code: AuthErrorCode.AccountNotVerified,
  message: 'verify your e-mail address before signing in',
  status: FORBIDDEN.status,
};
const TOO_SOON: ErrorResponse = {
  ...WRONG_CREDENTIALS,
  code: 'AUTH.VERIFICATION_RESEND_TOO_SOON',
  message: 'a verification link was sent less than 60 seconds ago; wait a moment',
  status: TOO_MANY.status,
  path: RESEND_URL,
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
    expect(elements(fixture, RESEND).length).toBe(NOTHING);
  });

  it('offers a new verification link to an unverified account and sends it for the typed credentials', () => {
    const fixture = failLoginWith(NOT_VERIFIED, FORBIDDEN);

    buttonLabelled(fixture, RESEND_BUTTON).click();
    const resend = httpMock.expectOne(RESEND_URL);
    expect(resend.request.body).toEqual({ email: TestUsers.email, password: TestUsers.password });
    resend.flush(null);
    TestBed.tick();

    expect(element(fixture, SUCCESS_NOTICE).textContent).toContain(RESEND_SENT);
  });

  it('says why a new link cannot go out yet and keeps the button', () => {
    const fixture = failLoginWith(NOT_VERIFIED, FORBIDDEN);

    buttonLabelled(fixture, RESEND_BUTTON).click();
    httpMock.expectOne(RESEND_URL).flush(TOO_SOON, TOO_MANY);
    TestBed.tick();

    expect((fixture.nativeElement as HTMLElement).textContent).toContain(TOO_SOON.message);
    expect(buttonLabelled(fixture, RESEND_BUTTON)).toBeTruthy();
  });

  function failLoginWith(
    body: ErrorResponse,
    status: typeof UNAUTHORIZED,
  ): ComponentFixture<LoginPage> {
    const fixture = render();
    fill(fixture);
    submitForm(fixture);
    httpMock.expectOne(LOGIN_URL).flush(body, status);
    TestBed.tick();
    return fixture;
  }
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
