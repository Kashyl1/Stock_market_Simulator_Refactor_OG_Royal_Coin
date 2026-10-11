import { inject } from '@angular/core';
import { CanActivateFn, Router, UrlTree } from '@angular/router';
import { AppPath } from '../core/app-routes';
import { AuthService } from './auth.service';

export const guestGuard: CanActivateFn = (): boolean | UrlTree => {
  if (inject(AuthService).isAuthenticated()) {
    return inject(Router).createUrlTree([AppPath.Dashboard]);
  }
  return true;
};
