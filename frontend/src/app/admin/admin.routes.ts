import { Routes } from '@angular/router';
import { pageTitle } from '../core/app-info';
import { AdminRoute } from '../core/app-routes';

export const routes: Routes = [
  {
    path: AdminRoute.Index,
    pathMatch: 'full',
    redirectTo: AdminRoute.Batch,
  },
  {
    path: AdminRoute.Batch,
    title: pageTitle('Batch jobs'),
    loadComponent: () => import('./batch/pages/batch-page').then((m) => m.BatchPage),
  },
  {
    path: AdminRoute.BatchJob,
    title: pageTitle('Batch job'),
    loadComponent: () => import('./batch/pages/batch-job-page').then((m) => m.BatchJobPage),
  },
];
