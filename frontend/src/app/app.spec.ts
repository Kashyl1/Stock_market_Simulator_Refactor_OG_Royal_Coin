import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { App, ConnectionStatus } from './app';
import { HEALTH_PATH, HealthResponse } from './core/backend.service';
import { environment } from '../environments/environment';

const HEALTH_URL = `${environment.apiUrl}${HEALTH_PATH}`;
const APP_TITLE = 'trading_simulator_refactor';
const TITLE_SELECTOR = 'h1';
const CARD_SELECTOR = '.card';
const STATUS_ATTRIBUTE = 'data-status';
const HEALTHY: HealthResponse = {
  status: 'UP',
  service: 'trading-simulator-backend',
  timestamp: '2026-09-10T12:00:00Z',
};
const SERVER_ERROR = { status: 500, statusText: 'Server Error' };
const SERVER_ERROR_BODY = 'boom';

describe('App', () => {
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('shows the backend as online when the health check answers', async () => {
    const fixture = TestBed.createComponent(App);
    TestBed.tick();
    httpMock.expectOne(HEALTH_URL).flush(HEALTHY);
    await fixture.whenStable();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector(TITLE_SELECTOR)?.textContent).toContain(APP_TITLE);
    expect(compiled.querySelector(CARD_SELECTOR)?.getAttribute(STATUS_ATTRIBUTE)).toBe(
      ConnectionStatus.Online,
    );
    expect(compiled.textContent).toContain(HEALTHY.service);
  });

  it('shows the backend as offline with the failure when the health check fails', async () => {
    const fixture = TestBed.createComponent(App);
    TestBed.tick();
    httpMock.expectOne(HEALTH_URL).flush(SERVER_ERROR_BODY, SERVER_ERROR);
    await fixture.whenStable();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector(CARD_SELECTOR)?.getAttribute(STATUS_ATTRIBUTE)).toBe(
      ConnectionStatus.Offline,
    );
    expect(compiled.textContent).toContain(`${SERVER_ERROR.status} ${SERVER_ERROR.statusText}`);
  });
});
