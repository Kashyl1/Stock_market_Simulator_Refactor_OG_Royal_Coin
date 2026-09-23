import { Routes } from '@angular/router';
import { authGuard } from './auth/auth.guard';
import { APP_NAME, pageTitle } from './core/app-info';
import { AppRoute } from './core/app-routes';

export const routes: Routes = [
  {
    path: AppRoute.Home,
    pathMatch: 'full',
    title: APP_NAME,
    loadComponent: () => import('./home/pages/home-page').then((m) => m.HomePage),
  },
  {
    path: AppRoute.Dashboard,
    title: pageTitle('Dashboard'),
    canActivate: [authGuard],
    loadComponent: () => import('./dashboard/pages/dashboard-page').then((m) => m.DashboardPage),
  },
  {
    path: AppRoute.Home,
    loadChildren: () => import('./auth/auth.routes').then((m) => m.routes),
  },
  {
    path: '**',
    redirectTo: AppRoute.Home,
  },
];
