import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { APP_ACCOUNT_NOTICE } from '../../core/app-info';
import { ErrorResponse } from '../../core/error-response';
import { UserStatus } from '../../core/user-status';
import { authUrl } from '../../testing/auth-session';
import { attributes, element, elements, submitForm, texts, typeInto } from '../../testing/dom';
import { TestUsers } from '../../testing/test-users';
import { REGISTER_PATH } from '../auth.service';
import { RegisterPage } from './register-page';

const REGISTER_URL = authUrl(REGISTER_PATH);
const EMAIL_INPUT = '#email';
const DISPLAY_NAME_INPUT = '#displayName';
const PASSWORD_INPUT = '#password';
const CONFIRM_PASSWORD_INPUT = '#confirmPassword';
const PASSWORD_INPUTS = '#password, #confirmPassword';
const PASSWORD_TOGGLE = '.password-visibility';
const TYPE_ATTRIBUTE = 'type';
const HIDDEN_TYPE = 'password';
const VISIBLE_TYPE = 'text';
const WRONG_CONFIRMATION = 'correct-horse-typo';
const MISMATCH_MESSAGE = 'Both passwords must be the same.';
const FIELD_ERROR = '.field__error';
const SUCCESS_NOTICE = '.notice--success';
const ERROR_NOTICE = '.notice--error';
const VERIFICATION_HINT = 'verification link';
const USER_ID = 7;
const CONFLICT = { status: 409, statusText: 'Conflict' };
const BAD_REQUEST = { status: 400, statusText: 'Bad Request' };
const TIMESTAMP = '2026-09-20T10:00:00Z';
const EMAIL_TAKEN: ErrorResponse = {
  code: 'AUTH.EMAIL_ALREADY_REGISTERED',
  message: 'e-mail is already registered',
  status: CONFLICT.status,
  timestamp: TIMESTAMP,
  path: REGISTER_URL,
  fieldErrors: [],
};
const WEAK_PASSWORD_MESSAGE = 'password must be at least 10 characters long';
const PASSWORD_REJECTED: ErrorResponse = {
  code: 'COMMON.VALIDATION_FAILED',
  message: 'validation failed',
  status: BAD_REQUEST.status,
  timestamp: TIMESTAMP,
  path: REGISTER_URL,
  fieldErrors: [{ field: 'password', message: WEAK_PASSWORD_MESSAGE }],
};

describe('RegisterPage', () => {
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RegisterPage],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('says up front that the account is simulated', () => {
    const fixture = render();

    expect((fixture.nativeElement as HTMLElement).textContent).toContain(APP_ACCOUNT_NOTICE);
  });

  it('keeps an incomplete form on the client and shows what is missing', () => {
    const fixture = render();

    submitForm(fixture);

    httpMock.expectNone(REGISTER_URL);
    expect(elements(fixture, FIELD_ERROR).length).toBeGreaterThan(0);
  });

  it('will not register when the two passwords differ', () => {
    const fixture = render();
    fill(fixture, WRONG_CONFIRMATION);

    submitForm(fixture);

    httpMock.expectNone(REGISTER_URL);
    expect(texts(fixture, FIELD_ERROR)).toContain(MISMATCH_MESSAGE);
  });

  it('reveals both passwords on request', () => {
    const fixture = render();

    expect(attributes(fixture, PASSWORD_INPUTS, TYPE_ATTRIBUTE)).toEqual([
      HIDDEN_TYPE,
      HIDDEN_TYPE,
    ]);

    element(fixture, PASSWORD_TOGGLE).click();
    TestBed.tick();

    expect(attributes(fixture, PASSWORD_INPUTS, TYPE_ATTRIBUTE)).toEqual([
      VISIBLE_TYPE,
      VISIBLE_TYPE,
    ]);
  });

  it('registers the account and asks the person to verify their e-mail', () => {
    const fixture = render();
    fill(fixture);

    submitForm(fixture);

    const call = httpMock.expectOne(REGISTER_URL);
    expect(call.request.body).toEqual({
      email: TestUsers.email,
      displayName: TestUsers.displayName,
      password: TestUsers.password,
    });
    call.flush({ userId: USER_ID, status: UserStatus.PendingVerification });
    TestBed.tick();

    const notice = element(fixture, SUCCESS_NOTICE);
    expect(notice.textContent).toContain(String(USER_ID));
    expect(notice.textContent).toContain(VERIFICATION_HINT);
  });

  it('shows the backend message when the e-mail is already registered', () => {
    const fixture = render();
    fill(fixture);

    submitForm(fixture);
    httpMock.expectOne(REGISTER_URL).flush(EMAIL_TAKEN, CONFLICT);
    TestBed.tick();

    expect(element(fixture, ERROR_NOTICE).textContent).toContain(EMAIL_TAKEN.message);
  });

  it('puts a field the backend rejected next to its input', () => {
    const fixture = render();
    fill(fixture);

    submitForm(fixture);
    httpMock.expectOne(REGISTER_URL).flush(PASSWORD_REJECTED, BAD_REQUEST);
    TestBed.tick();

    expect(texts(fixture, FIELD_ERROR)).toContain(WEAK_PASSWORD_MESSAGE);
  });
});

function render(): ComponentFixture<RegisterPage> {
  const fixture = TestBed.createComponent(RegisterPage);
  TestBed.tick();
  return fixture;
}

function fill(
  fixture: ComponentFixture<RegisterPage>,
  confirmation: string = TestUsers.password,
): void {
  typeInto(fixture, EMAIL_INPUT, TestUsers.email);
  typeInto(fixture, DISPLAY_NAME_INPUT, TestUsers.displayName);
  typeInto(fixture, PASSWORD_INPUT, TestUsers.password);
  typeInto(fixture, CONFIRM_PASSWORD_INPUT, confirmation);
}
