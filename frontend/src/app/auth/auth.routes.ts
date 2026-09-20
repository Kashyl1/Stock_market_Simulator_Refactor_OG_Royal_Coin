import { Routes } from '@angular/router';
import { pageTitle } from '../core/app-info';
import { AppRoute } from '../core/app-routes';

export const routes: Routes = [
  {
    path: AppRoute.Register,
    title: pageTitle('Create account'),
    loadComponent: () => import('./pages/register-page').then((m) => m.RegisterPage),
  },
  {
    path: AppRoute.Login,
    title: pageTitle('Log in'),
    loadComponent: () => import('./pages/login-page').then((m) => m.LoginPage),
  },
];
