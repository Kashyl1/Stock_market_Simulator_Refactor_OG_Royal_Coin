import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { ErrorResponse } from '../../core/error-response';
import { authUrl } from '../../testing/auth-session';
import { element } from '../../testing/dom';
import { VERIFY_EMAIL_PATH } from '../auth.service';
import { MISSING_TOKEN_MESSAGE, VerifyEmailPage } from './verify-email-page';

const VERIFY_URL = authUrl(VERIFY_EMAIL_PATH);
const TOKEN_INPUT = 'token';
const RAW_TOKEN = 'raw-token';
const SUCCESS_NOTICE = '.notice--success';
const ERROR_NOTICE = '.notice--error';
const BAD_REQUEST = { status: 400, statusText: 'Bad Request' };
const SPENT_TOKEN: ErrorResponse = {
  code: 'AUTH.VERIFICATION_TOKEN_INVALID',
  message: 'the verification token is not valid',
  status: BAD_REQUEST.status,
  timestamp: '2026-09-23T10:00:00Z',
  path: VERIFY_URL,
  fieldErrors: [],
};

describe('VerifyEmailPage', () => {
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [VerifyEmailPage],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('consumes the token from the link and reports the account as verified', () => {
    const fixture = renderWithToken(RAW_TOKEN);

    const call = httpMock.expectOne(VERIFY_URL);
    expect(call.request.body).toEqual({ token: RAW_TOKEN });
    call.flush(null);
    TestBed.tick();

    expect(element(fixture, SUCCESS_NOTICE).textContent).toContain('verified');
  });

  it('shows the backend message when the token was already used', () => {
    const fixture = renderWithToken(RAW_TOKEN);

    httpMock.expectOne(VERIFY_URL).flush(SPENT_TOKEN, BAD_REQUEST);
    TestBed.tick();

    expect(element(fixture, ERROR_NOTICE).textContent).toContain(SPENT_TOKEN.message);
  });

  it('says what is wrong when the link carries no token', () => {
    const fixture = TestBed.createComponent(VerifyEmailPage);
    TestBed.tick();

    httpMock.expectNone(VERIFY_URL);
    expect(element(fixture, ERROR_NOTICE).textContent).toContain(MISSING_TOKEN_MESSAGE);
  });
});

function renderWithToken(token: string): ComponentFixture<VerifyEmailPage> {
  const fixture = TestBed.createComponent(VerifyEmailPage);
  fixture.componentRef.setInput(TOKEN_INPUT, token);
  TestBed.tick();
  return fixture;
}
