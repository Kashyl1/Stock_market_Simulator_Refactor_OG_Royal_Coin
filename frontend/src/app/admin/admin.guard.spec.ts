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
import { TEST_ADMIN_ACCOUNT } from '../testing/test-accounts';
import { adminGuard } from './admin.guard';

const ROUTE = {} as ActivatedRouteSnapshot;
const STATE = { url: AppPath.AdminBatch } as RouterStateSnapshot;

describe('adminGuard', () => {
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('lets an admin through', () => {
    signIn(httpMock, TEST_ADMIN_ACCOUNT);

    expect(runGuard()).toBe(true);
  });

  it('sends anyone else to the dashboard', () => {
    signIn(httpMock);

    const result = runGuard();

    expect(result).toBeInstanceOf(UrlTree);
    expect(String(result)).toBe(AppPath.Dashboard);
  });

  function runGuard(): boolean | UrlTree {
    return TestBed.runInInjectionContext(() => adminGuard(ROUTE, STATE)) as boolean | UrlTree;
  }
});
