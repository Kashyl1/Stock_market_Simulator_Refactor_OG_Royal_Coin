import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { signIn } from '../../testing/auth-session';
import { element } from '../../testing/dom';
import { TestUsers } from '../../testing/test-users';
import { DashboardPage } from './dashboard-page';

const HEADING = 'h1';

describe('DashboardPage', () => {
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DashboardPage],
      providers: [provideHttpClient(), provideHttpClientTesting()],
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
  });
});
