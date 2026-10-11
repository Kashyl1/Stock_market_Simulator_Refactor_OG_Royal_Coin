import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import {
  ActivatedRouteSnapshot,
  RouterStateSnapshot,
  UrlTree,
  provideRouter,
} from '@angular/router';
import { AppPath } from '../core/app-routes';
import { signIn } from '../testing/auth-session';
import { guestGuard } from './guest.guard';

const ROUTE = {} as ActivatedRouteSnapshot;
const STATE = { url: AppPath.Login } as RouterStateSnapshot;

describe('guestGuard', () => {
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('lets a visitor in', () => {
    expect(runGuard()).toBe(true);
  });

  it('sends a signed in person to the dashboard', () => {
    signIn(httpMock);

    const result = runGuard();

    expect(result).toBeInstanceOf(UrlTree);
    expect(String(result)).toBe(AppPath.Dashboard);
  });

  function runGuard(): boolean | UrlTree {
    return TestBed.runInInjectionContext(() => guestGuard(ROUTE, STATE)) as boolean | UrlTree;
  }
});
