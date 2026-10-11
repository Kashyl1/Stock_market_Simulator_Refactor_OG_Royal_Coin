import { Routes } from '@angular/router';
import { pageTitle } from '../core/app-info';
import { AppPath, PortfolioRoute } from '../core/app-routes';

export const routes: Routes = [
  {
    path: PortfolioRoute.Index,
    pathMatch: 'full',
    redirectTo: AppPath.Dashboard,
  },
  {
    path: PortfolioRoute.New,
    title: pageTitle('New portfolio'),
    loadComponent: () => import('./pages/new-portfolio-page').then((m) => m.NewPortfolioPage),
  },
  {
    path: PortfolioRoute.Portfolio,
    title: pageTitle('Portfolio'),
    loadComponent: () => import('./pages/portfolio-page').then((m) => m.PortfolioPage),
  },
];
