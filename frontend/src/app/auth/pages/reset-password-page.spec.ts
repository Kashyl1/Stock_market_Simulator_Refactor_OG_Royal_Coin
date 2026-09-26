import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { ErrorResponse } from '../../core/error-response';
import { authUrl } from '../../testing/auth-session';
import { element, elements, submitForm, texts, typeInto } from '../../testing/dom';
import { TestUsers } from '../../testing/test-users';
import { RESET_PASSWORD_PATH } from '../auth.service';
import { MISSING_TOKEN_MESSAGE, ResetPasswordPage } from './reset-password-page';

const RESET_URL = authUrl(RESET_PASSWORD_PATH);
const TOKEN_INPUT = 'token';
const RAW_TOKEN = 'raw-token';
const NEW_PASSWORD_INPUT = '#newPassword';
const CONFIRM_PASSWORD_INPUT = '#confirmPassword';
const FIELD_ERROR = '.field__error';
const SUCCESS_NOTICE = '.notice--success';
const ERROR_NOTICE = '.notice--error';
const MISMATCH_MESSAGE = 'Both passwords must be the same.';
const WRONG_CONFIRMATION = 'another-horse-typo';
const BAD_REQUEST = { status: 400, statusText: 'Bad Request' };
const SPENT_TOKEN: ErrorResponse = {
  code: 'AUTH.RESET_TOKEN_EXPIRED',
  message: 'the password reset token has expired',
  status: BAD_REQUEST.status,
  timestamp: '2026-09-26T10:00:00Z',
  path: RESET_URL,
  fieldErrors: [],
};

describe('ResetPasswordPage', () => {
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ResetPasswordPage],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('refuses to show the form when the link carries no token', () => {
    const fixture = TestBed.createComponent(ResetPasswordPage);
    TestBed.tick();

    expect(element(fixture, ERROR_NOTICE).textContent).toContain(MISSING_TOKEN_MESSAGE);
    expect(elements(fixture, NEW_PASSWORD_INPUT).length).toBe(0);
  });

  it('keeps a mismatched pair on the client', () => {
    const fixture = render();
    fill(fixture, WRONG_CONFIRMATION);

    submitForm(fixture);

    httpMock.expectNone(RESET_URL);
    expect(texts(fixture, FIELD_ERROR)).toContain(MISMATCH_MESSAGE);
  });

  it('sends the token with the new password and confirms the change', () => {
    const fixture = render();
    fill(fixture, TestUsers.password);

    submitForm(fixture);

    const call = httpMock.expectOne(RESET_URL);
    expect(call.request.body).toEqual({ token: RAW_TOKEN, newPassword: TestUsers.password });
    call.flush(null);
    TestBed.tick();

    expect(element(fixture, SUCCESS_NOTICE).textContent).toContain('changed');
    expect(elements(fixture, NEW_PASSWORD_INPUT).length).toBe(0);
  });

  it('shows the backend message when the token is spent', () => {
    const fixture = render();
    fill(fixture, TestUsers.password);

    submitForm(fixture);
    httpMock.expectOne(RESET_URL).flush(SPENT_TOKEN, BAD_REQUEST);
    TestBed.tick();

    expect(element(fixture, ERROR_NOTICE).textContent).toContain(SPENT_TOKEN.message);
  });
});

function render(): ComponentFixture<ResetPasswordPage> {
  const fixture = TestBed.createComponent(ResetPasswordPage);
  fixture.componentRef.setInput(TOKEN_INPUT, RAW_TOKEN);
  TestBed.tick();
  return fixture;
}

function fill(fixture: ComponentFixture<ResetPasswordPage>, confirmation: string): void {
  typeInto(fixture, NEW_PASSWORD_INPUT, TestUsers.password);
  typeInto(fixture, CONFIRM_PASSWORD_INPUT, confirmation);
}
