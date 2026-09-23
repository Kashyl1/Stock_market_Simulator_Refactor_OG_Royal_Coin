import { HttpErrorResponse, HttpInterceptorFn, HttpRequest } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, switchMap, throwError } from 'rxjs';
import { environment } from '../../environments/environment';
import { AppPath } from '../core/app-routes';
import { AUTH_PATH, AuthService, PUBLIC_AUTH_PATHS } from './auth.service';

const UNAUTHORIZED = 401;
const AUTHORIZATION_HEADER = 'Authorization';
const BEARER_PREFIX = 'Bearer ';

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  if (!needsAccessToken(request.url)) {
    return next(request);
  }

  return next(withAccessToken(request, auth.accessToken())).pipe(
    catchError((failure: HttpErrorResponse) => {
      if (failure.status !== UNAUTHORIZED) {
        return throwError(() => failure);
      }
      return auth.refreshAccessToken().pipe(
        switchMap((token) => next(withAccessToken(request, token))),
        catchError((refreshFailure: HttpErrorResponse) => {
          auth.clearSession();
          router.navigate([AppPath.Login]);
          return throwError(() => refreshFailure);
        }),
      );
    }),
  );
};

function needsAccessToken(url: string): boolean {
  if (!url.startsWith(environment.apiUrl)) {
    return false;
  }
  return !PUBLIC_AUTH_PATHS.some((path) => url === `${environment.apiUrl}${AUTH_PATH}${path}`);
}

function withAccessToken<T>(request: HttpRequest<T>, token: string | null): HttpRequest<T> {
  if (!token) {
    return request;
  }
  return request.clone({ setHeaders: { [AUTHORIZATION_HEADER]: `${BEARER_PREFIX}${token}` } });
}
