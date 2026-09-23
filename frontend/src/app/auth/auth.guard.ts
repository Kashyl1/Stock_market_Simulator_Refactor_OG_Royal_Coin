import { inject } from '@angular/core';
import { CanActivateFn, Router, UrlTree } from '@angular/router';
import { AppPath } from '../core/app-routes';
import { AuthService } from './auth.service';

export const REDIRECT_PARAM = 'redirect';

export const authGuard: CanActivateFn = (route, state): boolean | UrlTree => {
  const auth = inject(AuthService);
  const router = inject(Router);

  if (auth.isAuthenticated()) {
    return true;
  }
  return router.createUrlTree([AppPath.Login], { queryParams: { [REDIRECT_PARAM]: state.url } });
};
