import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { portfolioPath } from '../../core/app-routes';
import { element, elements, selectOption, submitForm, typeInto } from '../../testing/dom';
import {
  TEST_CURRENCIES,
  TEST_PORTFOLIO,
  TEST_PORTFOLIO_CURRENCY,
  TEST_PORTFOLIO_NAME,
  apiUrl,
} from '../../testing/test-money';
import { DEFAULT_PORTFOLIO_CURRENCY } from '../portfolio-models';
import { CURRENCIES_PATH, PORTFOLIOS_PATH } from '../portfolio.service';
import { NewPortfolioPage } from './new-portfolio-page';

const NAME = '#portfolio-name';
const CURRENCY = '#portfolio-currency';
const FIELD_ERROR = '.field__error';
const ERROR_NOTICE = '.notice--error';
const POST = 'POST';
const NAME_TAKEN_MESSAGE = `you already have a portfolio called ${TEST_PORTFOLIO_NAME}`;
const CONFLICT = { status: 409, statusText: 'Conflict' };
const NAME_TAKEN_BODY = {
  code: 'PORTFOLIO.NAME_TAKEN',
  message: NAME_TAKEN_MESSAGE,
  status: CONFLICT.status,
  timestamp: '2026-10-11T10:00:00Z',
  path: '/api/portfolios',
  fieldErrors: [],
};

describe('NewPortfolioPage', () => {
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [NewPortfolioPage],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    vi.restoreAllMocks();
    httpMock.verify();
  });

  it('offers every currency with the zloty chosen', async () => {
    const fixture = await render();

    expect(elements(fixture, `${CURRENCY} option`).length).toBe(TEST_CURRENCIES.length);
    expect(element<HTMLSelectElement>(fixture, CURRENCY).value).toBe(DEFAULT_PORTFOLIO_CURRENCY);
  });

  it('creates the portfolio and opens it', async () => {
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    const fixture = await render();

    typeInto(fixture, NAME, TEST_PORTFOLIO_NAME);
    selectOption(fixture, CURRENCY, TEST_PORTFOLIO_CURRENCY);
    submitForm(fixture);
    const create = httpMock.expectOne(apiUrl(PORTFOLIOS_PATH));
    expect(create.request.method).toBe(POST);
    expect(create.request.body).toEqual({
      name: TEST_PORTFOLIO_NAME,
      currency: TEST_PORTFOLIO_CURRENCY,
    });
    create.flush(TEST_PORTFOLIO);
    await fixture.whenStable();

    expect(navigate).toHaveBeenCalledWith(portfolioPath(TEST_PORTFOLIO.id));
  });

  it('asks for a name before creating anything', async () => {
    const fixture = await render();

    submitForm(fixture);

    httpMock.expectNone(apiUrl(PORTFOLIOS_PATH));
    expect(elements(fixture, FIELD_ERROR).length).toBe(1);
  });

  it('says when the name is already taken', async () => {
    const fixture = await render();

    typeInto(fixture, NAME, TEST_PORTFOLIO_NAME);
    submitForm(fixture);
    httpMock.expectOne(apiUrl(PORTFOLIOS_PATH)).flush(NAME_TAKEN_BODY, CONFLICT);
    await fixture.whenStable();

    expect(element(fixture, ERROR_NOTICE).textContent).toContain(NAME_TAKEN_MESSAGE);
  });

  async function render(): Promise<ComponentFixture<NewPortfolioPage>> {
    const fixture = TestBed.createComponent(NewPortfolioPage);
    TestBed.tick();
    httpMock.expectOne(apiUrl(CURRENCIES_PATH)).flush(TEST_CURRENCIES);
    await fixture.whenStable();
    return fixture;
  }
});
