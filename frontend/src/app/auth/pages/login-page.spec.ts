import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { element, elements, submitForm, typeInto } from '../../testing/dom';
import { TestUsers } from '../../testing/test-users';
import { LoginPage, SIGN_IN_UNAVAILABLE_MESSAGE } from './login-page';

const EMAIL_INPUT = '#email';
const PASSWORD_INPUT = '#password';
const FIELD_ERROR = '.field__error';
const NOTICE = '.notice';

describe('LoginPage', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LoginPage],
      providers: [provideRouter([])],
    }).compileComponents();
  });

  it('asks for the missing credentials before doing anything', () => {
    const fixture = render();

    submitForm(fixture);

    expect(elements(fixture, FIELD_ERROR).length).toBeGreaterThan(0);
    expect(elements(fixture, NOTICE).length).toBe(0);
  });

  it('says that signing in is not live yet once the form is filled in', () => {
    const fixture = render();
    typeInto(fixture, EMAIL_INPUT, TestUsers.email);
    typeInto(fixture, PASSWORD_INPUT, TestUsers.password);

    submitForm(fixture);

    expect(element(fixture, NOTICE).textContent).toContain(SIGN_IN_UNAVAILABLE_MESSAGE);
  });
});

function render(): ComponentFixture<LoginPage> {
  const fixture = TestBed.createComponent(LoginPage);
  TestBed.tick();
  return fixture;
}
