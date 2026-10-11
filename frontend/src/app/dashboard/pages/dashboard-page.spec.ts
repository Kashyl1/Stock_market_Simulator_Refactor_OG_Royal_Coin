import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { AppPath } from '../../core/app-routes';
import { signIn } from '../../testing/auth-session';
import { attributes, element } from '../../testing/dom';
import { TEST_ADMIN_ACCOUNT } from '../../testing/test-accounts';
import { TestUsers } from '../../testing/test-users';
import { DashboardPage } from './dashboard-page';

const HEADING = 'h1';
const LINK = 'a';
const HREF = 'href';

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

  it('greets the signed in person by their display name and shows the account', () => {
    signIn(httpMock);

    const fixture = TestBed.createComponent(DashboardPage);
    TestBed.tick();

    expect(element(fixture, HEADING).textContent).toContain(TestUsers.displayName);
    expect((fixture.nativeElement as HTMLElement).textContent).toContain(TestUsers.email);
    expect(attributes(fixture, LINK, HREF)).toEqual([]);
  });

  it('points an admin to the batch jobs', () => {
    signIn(httpMock, TEST_ADMIN_ACCOUNT);

    const fixture = TestBed.createComponent(DashboardPage);
    TestBed.tick();

    expect(attributes(fixture, LINK, HREF)).toEqual([AppPath.AdminBatch]);
  });
});
