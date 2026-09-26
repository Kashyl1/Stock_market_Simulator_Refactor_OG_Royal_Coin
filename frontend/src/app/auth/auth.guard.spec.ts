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
import { REDIRECT_PARAM, authGuard } from './auth.guard';

const ROUTE = {} as ActivatedRouteSnapshot;
const STATE = { url: AppPath.Dashboard } as RouterStateSnapshot;

describe('authGuard', () => {
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('sends a signed out visitor to the login page and remembers where they wanted to go', () => {
    const result = runGuard();

    expect(result).toBeInstanceOf(UrlTree);
    expect(String(result)).toContain(AppPath.Login);
    expect(String(result)).toContain(`${REDIRECT_PARAM}=`);
  });

  it('lets a signed in person through', () => {
    signIn(httpMock);

    expect(runGuard()).toBe(true);
  });

  function runGuard(): boolean | UrlTree {
    return TestBed.runInInjectionContext(() => authGuard(ROUTE, STATE)) as boolean | UrlTree;
  }
});
