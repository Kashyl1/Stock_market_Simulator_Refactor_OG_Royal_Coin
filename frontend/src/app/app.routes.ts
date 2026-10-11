import { Routes } from '@angular/router';
import { adminGuard } from './admin/admin.guard';
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
    path: AppRoute.Admin,
    canActivate: [authGuard, adminGuard],
    loadChildren: () => import('./admin/admin.routes').then((m) => m.routes),
  },
  {
    path: AppRoute.Home,
    loadChildren: () => import('./auth/auth.routes').then((m) => m.routes),
  },
  {
    path: AppRoute.NotFound,
    title: pageTitle('Page not found'),
    loadComponent: () => import('./not-found/pages/not-found-page').then((m) => m.NotFoundPage),
  },
];
