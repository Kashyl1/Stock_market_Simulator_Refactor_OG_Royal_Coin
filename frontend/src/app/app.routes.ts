import { Routes } from '@angular/router';
import { APP_NAME } from './core/app-info';
import { AppRoute } from './core/app-routes';

export const routes: Routes = [
  {
    path: AppRoute.Home,
    pathMatch: 'full',
    title: APP_NAME,
    loadComponent: () => import('./home/pages/home-page').then((m) => m.HomePage),
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
