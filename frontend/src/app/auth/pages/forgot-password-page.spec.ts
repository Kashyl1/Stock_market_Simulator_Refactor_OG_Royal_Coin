import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { authUrl } from '../../testing/auth-session';
import { element, elements, submitForm, typeInto } from '../../testing/dom';
import { TestUsers } from '../../testing/test-users';
import { FORGOT_PASSWORD_PATH } from '../auth.service';
import { ForgotPasswordPage, RESET_REQUESTED_MESSAGE } from './forgot-password-page';

const FORGOT_URL = authUrl(FORGOT_PASSWORD_PATH);
const EMAIL_INPUT = '#email';
const FIELD_ERROR = '.field__error';
const SUCCESS_NOTICE = '.notice--success';
const ACCEPTED = { status: 202, statusText: 'Accepted' };

describe('ForgotPasswordPage', () => {
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ForgotPasswordPage],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('asks for an address before calling the backend', () => {
    const fixture = render();

    submitForm(fixture);

    httpMock.expectNone(FORGOT_URL);
    expect(elements(fixture, FIELD_ERROR).length).toBeGreaterThan(0);
  });

  it('sends the address and answers without saying whether the account exists', () => {
    const fixture = render();
    typeInto(fixture, EMAIL_INPUT, TestUsers.email);

    submitForm(fixture);

    const call = httpMock.expectOne(FORGOT_URL);
    expect(call.request.body).toEqual({ email: TestUsers.email });
    call.flush(null, ACCEPTED);
    TestBed.tick();

    expect(element(fixture, SUCCESS_NOTICE).textContent).toContain(RESET_REQUESTED_MESSAGE);
  });
});

function render(): ComponentFixture<ForgotPasswordPage> {
  const fixture = TestBed.createComponent(ForgotPasswordPage);
  TestBed.tick();
  return fixture;
}
