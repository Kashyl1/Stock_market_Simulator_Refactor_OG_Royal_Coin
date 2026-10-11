import { inject } from '@angular/core';
import { CanActivateFn, Router, UrlTree } from '@angular/router';
import { AuthService } from '../auth/auth.service';
import { AppPath } from '../core/app-routes';

export const adminGuard: CanActivateFn = (): boolean | UrlTree => {
  if (inject(AuthService).isAdmin()) {
    return true;
  }
  return inject(Router).createUrlTree([AppPath.Dashboard]);
};
