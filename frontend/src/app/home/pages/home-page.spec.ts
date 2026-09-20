import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { environment } from '../../../environments/environment';
import { APP_DISCLAIMER, APP_LEGAL_NOTICE, APP_NAME } from '../../core/app-info';
import { AppPath } from '../../core/app-routes';
import { HEALTH_PATH, HealthResponse } from '../../core/backend.service';
import { ConnectionStatus } from '../../core/connection-status';
import { attributes, element, elements } from '../../testing/dom';
import { HomePage } from './home-page';

const HEALTH_URL = `${environment.apiUrl}${HEALTH_PATH}`;
const HEADING = 'h1';
const CALL_TO_ACTION = '.hero__actions a';
const FEATURE = '.feature';
const STATUS = '.status';
const STATUS_LABEL = '.status__label';
const ONLINE_LABEL = 'Online';
const OFFLINE_LABEL = 'Offline';
const STATUS_ATTRIBUTE = 'data-status';
const HREF = 'href';
const FEATURE_COUNT = 3;
const HEALTHY: HealthResponse = {
  status: 'UP',
  service: 'trading-simulator-backend',
  timestamp: '2026-09-10T12:00:00Z',
};
const SERVER_ERROR = { status: 500, statusText: 'Server Error' };
const SERVER_ERROR_BODY = 'boom';

describe('HomePage', () => {
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HomePage],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('pitches the product and offers registration and log in', async () => {
    const fixture = await renderWith(HEALTHY);

    expect(element(fixture, HEADING).textContent).toContain(APP_NAME);
    expect(elements(fixture, FEATURE).length).toBe(FEATURE_COUNT);
    expect(attributes(fixture, CALL_TO_ACTION, HREF)).toEqual([AppPath.Register, AppPath.Login]);
  });

  it('states that it is a simulation and not advice', async () => {
    const fixture = await renderWith(HEALTHY);

    const page = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(page).toContain(APP_DISCLAIMER);
    expect(page).toContain(APP_LEGAL_NOTICE);
  });

  it('shows the backend as online when the health check answers', async () => {
    const fixture = await renderWith(HEALTHY);

    expect(element(fixture, STATUS).getAttribute(STATUS_ATTRIBUTE)).toBe(ConnectionStatus.Online);
    expect(element(fixture, STATUS_LABEL).textContent).toContain(ONLINE_LABEL);
    expect((fixture.nativeElement as HTMLElement).textContent).toContain(HEALTHY.service);
  });

  it('shows the backend as offline with the failure when the health check fails', async () => {
    const fixture = TestBed.createComponent(HomePage);
    TestBed.tick();
    httpMock.expectOne(HEALTH_URL).flush(SERVER_ERROR_BODY, SERVER_ERROR);
    await fixture.whenStable();

    expect(element(fixture, STATUS).getAttribute(STATUS_ATTRIBUTE)).toBe(ConnectionStatus.Offline);
    expect(element(fixture, STATUS_LABEL).textContent).toContain(OFFLINE_LABEL);
    expect((fixture.nativeElement as HTMLElement).textContent).toContain(
      `${SERVER_ERROR.status} ${SERVER_ERROR.statusText}`,
    );
  });
});

async function renderWith(health: HealthResponse): Promise<ComponentFixture<HomePage>> {
  const fixture = TestBed.createComponent(HomePage);
  TestBed.tick();
  TestBed.inject(HttpTestingController).expectOne(HEALTH_URL).flush(health);
  await fixture.whenStable();
  return fixture;
}
