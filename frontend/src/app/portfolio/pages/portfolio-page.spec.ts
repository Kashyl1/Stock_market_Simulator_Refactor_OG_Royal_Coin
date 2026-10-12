import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  TestRequest,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { ACCOUNT_PATH } from '../../account/account.service';
import { PORTFOLIO_ID_PARAM } from '../../core/app-routes';
import { buttonLabelled, element, elements, submitForm, typeInto } from '../../testing/dom';
import { onePage } from '../../testing/test-batch';
import {
  TEST_MAIN_ACCOUNT,
  TEST_DEPOSITED,
  TEST_PORTFOLIO,
  TEST_PORTFOLIO_ID,
  TEST_PORTFOLIO_NAME,
  TEST_PORTFOLIO_PERFORMANCE,
  TEST_QUOTE,
  TEST_TRANSFER,
  apiUrl,
} from '../../testing/test-money';
import { DIRECTION_LABELS, PortfolioView, TransferDirection } from '../portfolio-models';
import {
  PORTFOLIOS_PATH,
  PortfolioParam,
  QUOTE_PATH,
  RECENT_TRANSFERS,
  TRANSFERS_PATH,
} from '../portfolio.service';
import { PortfolioPage } from './portfolio-page';

const ARRIVED_DOLLARS = '99.50 USD';
const SECOND_AMOUNT = '500';
const SECOND_ARRIVAL = 124.38;
const SECOND_ARRIVAL_TEXT = '124.38 USD';
const GET = 'GET';
const POST = 'POST';
const HEADING = 'h1';
const AMOUNT = '#transfer-amount';
const PREVIEW = '.transfer__preview';
const TRANSFER_ROW = 'tr.transfer';
const SUCCESS_NOTICE = '.notice--success';
const ERROR_NOTICE = '.notice--error';
const PORTFOLIO_URL = apiUrl(`${PORTFOLIOS_PATH}/${TEST_PORTFOLIO_ID}`);
const TRANSFERS_URL = `${PORTFOLIO_URL}${TRANSFERS_PATH}`;
const QUOTE_URL = `${TRANSFERS_URL}${QUOTE_PATH}`;
const NOT_FOUND = { status: 404, statusText: 'Not Found' };
const UNPROCESSABLE = { status: 422, statusText: 'Unprocessable Content' };
const NOT_ENOUGH_MESSAGE = 'only 600.00 PLN is available, 1500 PLN is needed';
const NOT_ENOUGH_BODY = {
  code: 'WALLET.INSUFFICIENT_FUNDS',
  message: NOT_ENOUGH_MESSAGE,
  status: UNPROCESSABLE.status,
  timestamp: '2026-10-11T10:00:00Z',
  path: TRANSFERS_URL,
  fieldErrors: [],
};
const TOO_MUCH = '1500';
const STAT = '.stat';
const STAT_VALUE = '.stat__value';
const STAT_NOTE = '.stat__note';
const NET_DEPOSITS = 'Net deposits';
const TAKEN_OUT = 150;
const PARTLY_WITHDRAWN: PortfolioView = {
  ...TEST_PORTFOLIO,
  performance: {
    ...TEST_PORTFOLIO_PERFORMANCE,
    takenOut: TAKEN_OUT,
    netDeposits: TEST_DEPOSITED - TAKEN_OUT,
  },
};
const NET_DEPOSITS_TEXT = '250.00 PLN';
const PUT_IN_TEXT = '400.00 PLN in';
const TAKEN_OUT_TEXT = '150.00 PLN out';

describe('PortfolioPage', () => {
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PortfolioPage],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('shows the portfolio, its value in zloty and its transfers', async () => {
    const fixture = await render();

    const page = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(element(fixture, HEADING).textContent).toContain(TEST_PORTFOLIO_NAME);
    expect(page).toContain(ARRIVED_DOLLARS);
    expect(page).toContain('389.36 PLN');
    expect(page).toContain('(-2.66%)');
    expect(elements(fixture, TRANSFER_ROW).length).toBe(1);
  });

  it('shows what went in and out as net deposits', async () => {
    const fixture = await render(PARTLY_WITHDRAWN);

    const tile = elements(fixture, STAT).find((stat) => stat.textContent?.includes(NET_DEPOSITS));
    expect(tile?.querySelector(STAT_VALUE)?.textContent).toContain(NET_DEPOSITS_TEXT);
    expect(tile?.querySelector(STAT_NOTE)?.textContent).toContain(PUT_IN_TEXT);
    expect(tile?.querySelector(STAT_NOTE)?.textContent).toContain(TAKEN_OUT_TEXT);
  });

  it('previews what arrives before the money moves', async () => {
    const fixture = await render();

    typeInto(fixture, AMOUNT, String(TEST_DEPOSITED));
    const quote = httpMock.expectOne((request) => request.url === QUOTE_URL);
    expect(quote.request.params.get(PortfolioParam.Direction)).toBe(TransferDirection.In);
    expect(quote.request.params.get(PortfolioParam.Amount)).toBe(String(TEST_DEPOSITED));
    quote.flush(TEST_QUOTE);
    await fixture.whenStable();

    expect(element(fixture, PREVIEW).textContent).toContain('2.00 PLN');
    expect(element(fixture, PREVIEW).textContent).toContain(ARRIVED_DOLLARS);
  });

  it('keeps the preview in place while a new amount is quoted', async () => {
    const fixture = await render();
    typeInto(fixture, AMOUNT, String(TEST_DEPOSITED));
    httpMock.expectOne((request) => request.url === QUOTE_URL).flush(TEST_QUOTE);
    await fixture.whenStable();

    typeInto(fixture, AMOUNT, SECOND_AMOUNT);
    const second = httpMock.expectOne((request) => request.url === QUOTE_URL);

    expect(element(fixture, PREVIEW).textContent).toContain(ARRIVED_DOLLARS);
    second.flush({ ...TEST_QUOTE, amount: SECOND_ARRIVAL });
    await fixture.whenStable();
    expect(element(fixture, PREVIEW).textContent).toContain(SECOND_ARRIVAL_TEXT);
  });

  it('withdraws in the currency of the portfolio', async () => {
    const fixture = await render();

    buttonLabelled(fixture, DIRECTION_LABELS[TransferDirection.Out]).click();
    TestBed.tick();
    typeInto(fixture, AMOUNT, String(TEST_DEPOSITED));
    const quote = httpMock.expectOne((request) => request.url === QUOTE_URL);

    expect(quote.request.params.get(PortfolioParam.Direction)).toBe(TransferDirection.Out);
    expect(element(fixture, `label[for="transfer-amount"]`).textContent).toContain(
      TEST_PORTFOLIO.currency,
    );
    quote.flush({ ...TEST_QUOTE, direction: TransferDirection.Out });
  });

  it('moves the money and shows the new state', async () => {
    const fixture = await render();

    typeInto(fixture, AMOUNT, String(TEST_DEPOSITED));
    httpMock.expectOne((request) => request.url === QUOTE_URL).flush(TEST_QUOTE);
    submitForm(fixture);
    const transfer = httpMock.expectOne(
      (request) => request.url === TRANSFERS_URL && request.method === POST,
    );
    expect(transfer.request.body).toEqual({
      direction: TransferDirection.In,
      amount: TEST_DEPOSITED,
    });
    transfer.flush(TEST_TRANSFER);
    TestBed.tick();
    answerPageLoad();
    await fixture.whenStable();

    expect(element(fixture, SUCCESS_NOTICE).textContent).toContain(ARRIVED_DOLLARS);
    expect(element(fixture, SUCCESS_NOTICE).textContent).toContain(TEST_PORTFOLIO_NAME);
  });

  it('says why a transfer was refused', async () => {
    const fixture = await render();

    typeInto(fixture, AMOUNT, TOO_MUCH);
    httpMock.expectOne((request) => request.url === QUOTE_URL).flush(TEST_QUOTE);
    submitForm(fixture);
    httpMock
      .expectOne((request) => request.url === TRANSFERS_URL && request.method === POST)
      .flush(NOT_ENOUGH_BODY, UNPROCESSABLE);
    await fixture.whenStable();

    expect(element(fixture, ERROR_NOTICE).textContent).toContain(NOT_ENOUGH_MESSAGE);
  });

  it('says when the portfolio does not exist', async () => {
    const fixture = TestBed.createComponent(PortfolioPage);
    fixture.componentRef.setInput(PORTFOLIO_ID_PARAM, String(TEST_PORTFOLIO_ID));
    TestBed.tick();
    httpMock.expectOne(PORTFOLIO_URL).flush(null, NOT_FOUND);
    httpMock.expectOne(apiUrl(ACCOUNT_PATH)).flush(TEST_MAIN_ACCOUNT);
    transfersRequest().flush(null, NOT_FOUND);
    await fixture.whenStable();

    expect(element(fixture, HEADING).textContent).toContain('Portfolio not found');
  });

  async function render(portfolio = TEST_PORTFOLIO): Promise<ComponentFixture<PortfolioPage>> {
    const fixture = TestBed.createComponent(PortfolioPage);
    fixture.componentRef.setInput(PORTFOLIO_ID_PARAM, String(TEST_PORTFOLIO_ID));
    TestBed.tick();
    answerPageLoad(portfolio);
    await fixture.whenStable();
    return fixture;
  }

  function answerPageLoad(portfolio = TEST_PORTFOLIO): void {
    httpMock.expectOne(PORTFOLIO_URL).flush(portfolio);
    httpMock.expectOne(apiUrl(ACCOUNT_PATH)).flush(TEST_MAIN_ACCOUNT);
    const transfers = transfersRequest();
    expect(transfers.request.params.get(PortfolioParam.Size)).toBe(String(RECENT_TRANSFERS));
    transfers.flush(onePage([TEST_TRANSFER]));
  }

  function transfersRequest(): TestRequest {
    return httpMock.expectOne((request) => request.url === TRANSFERS_URL && request.method === GET);
  }
});
