import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import {
  ACCOUNT_PATH,
  AccountParam,
  ENTRIES_PATH,
  RECENT_ENTRIES,
} from '../../account/account.service';
import { AccountView } from '../../account/account-models';
import { AppPath, portfolioPath } from '../../core/app-routes';
import { FIRST_PAGE } from '../../core/page-response';
import { PORTFOLIOS_PATH } from '../../portfolio/portfolio.service';
import { PortfolioView } from '../../portfolio/portfolio-models';
import { signIn } from '../../testing/auth-session';
import { attributes, element, elements } from '../../testing/dom';
import { TEST_ADMIN_ACCOUNT } from '../../testing/test-accounts';
import {
  TEST_MAIN_ACCOUNT,
  TEST_DEPOSIT_ENTRY,
  TEST_PORTFOLIO,
  TEST_PORTFOLIO_NAME,
  TEST_TRANSFER_ENTRY,
  apiUrl,
} from '../../testing/test-money';
import { onePage } from '../../testing/test-batch';
import { TestUsers } from '../../testing/test-users';
import { DashboardPage } from './dashboard-page';

const HEADING = 'h1';
const LINK = 'a';
const HREF = 'href';
const MAIN_ACCOUNT = '.account';
const PORTFOLIO_CARD = '.portfolio';
const MOVEMENT = '.entry';
const NO_PORTFOLIOS = 'No portfolios yet';

describe('DashboardPage', () => {
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DashboardPage],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('greets the signed in person and shows the main account with its result', async () => {
    const fixture = await render();

    expect(element(fixture, HEADING).textContent).toContain(TestUsers.displayName);
    expect((fixture.nativeElement as HTMLElement).textContent).toContain(TestUsers.email);
    const mainAccount = element(fixture, MAIN_ACCOUNT).textContent;
    expect(mainAccount).toContain('600.00 PLN');
    expect(mainAccount).toContain('989.36 PLN');
    expect(mainAccount).toContain('(-1.06%)');
  });

  it('lists the portfolios, each leading to its page', async () => {
    const fixture = await render();

    expect(elements(fixture, PORTFOLIO_CARD).length).toBe(1);
    expect(element(fixture, PORTFOLIO_CARD).textContent).toContain(TEST_PORTFOLIO_NAME);
    expect(element(fixture, PORTFOLIO_CARD).getAttribute(HREF)).toBe(
      portfolioPath(TEST_PORTFOLIO.id),
    );
    expect(attributes(fixture, LINK, HREF)).toContain(AppPath.NewPortfolio);
  });

  it('invites someone without portfolios to create one', async () => {
    const fixture = await render([]);

    expect((fixture.nativeElement as HTMLElement).textContent).toContain(NO_PORTFOLIOS);
  });

  it('shows the latest movements and where the money went', async () => {
    const fixture = await render();

    expect(elements(fixture, MOVEMENT).length).toBe(2);
    expect(elements(fixture, MOVEMENT)[0].textContent).toContain(TEST_PORTFOLIO_NAME);
  });

  it('points an admin to the batch jobs', async () => {
    const fixture = await render([TEST_PORTFOLIO], TEST_MAIN_ACCOUNT, true);

    expect(attributes(fixture, LINK, HREF)).toContain(AppPath.AdminBatch);
  });

  async function render(
    portfolios: readonly PortfolioView[] = [TEST_PORTFOLIO],
    account: AccountView = TEST_MAIN_ACCOUNT,
    asAdmin = false,
  ): Promise<ComponentFixture<DashboardPage>> {
    signIn(httpMock, asAdmin ? TEST_ADMIN_ACCOUNT : undefined);
    const fixture = TestBed.createComponent(DashboardPage);
    TestBed.tick();
    httpMock.expectOne(apiUrl(ACCOUNT_PATH)).flush(account);
    const entries = httpMock.expectOne(
      (request) => request.url === apiUrl(`${ACCOUNT_PATH}${ENTRIES_PATH}`),
    );
    expect(entries.request.params.get(AccountParam.Page)).toBe(String(FIRST_PAGE));
    expect(entries.request.params.get(AccountParam.Size)).toBe(String(RECENT_ENTRIES));
    entries.flush(onePage([TEST_TRANSFER_ENTRY, TEST_DEPOSIT_ENTRY]));
    httpMock.expectOne(apiUrl(PORTFOLIOS_PATH)).flush(portfolios);
    await fixture.whenStable();
    return fixture;
  }
});
