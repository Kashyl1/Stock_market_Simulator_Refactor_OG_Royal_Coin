const ADMIN_SEGMENT = 'admin';
const BATCH_SEGMENT = 'batch';
const JOBS_SEGMENT = 'jobs';

export const BATCH_JOB_ID_PARAM = 'id';

export const AppRoute = {
  Home: '',
  Register: 'register',
  Login: 'login',
  VerifyEmail: 'verify-email',
  ForgotPassword: 'forgot-password',
  ResetPassword: 'reset-password',
  Dashboard: 'dashboard',
  Admin: ADMIN_SEGMENT,
  NotFound: '**',
} as const;

export type AppRoute = (typeof AppRoute)[keyof typeof AppRoute];

export const AdminRoute = {
  Index: '',
  Batch: BATCH_SEGMENT,
  BatchJob: `${BATCH_SEGMENT}/${JOBS_SEGMENT}/:${BATCH_JOB_ID_PARAM}`,
} as const;

export const AppPath = {
  Home: '/',
  Register: `/${AppRoute.Register}`,
  Login: `/${AppRoute.Login}`,
  VerifyEmail: `/${AppRoute.VerifyEmail}`,
  ForgotPassword: `/${AppRoute.ForgotPassword}`,
  ResetPassword: `/${AppRoute.ResetPassword}`,
  Dashboard: `/${AppRoute.Dashboard}`,
  AdminBatch: `/${ADMIN_SEGMENT}/${BATCH_SEGMENT}`,
} as const;

export const batchJobPath = (id: number): string => `${AppPath.AdminBatch}/${JOBS_SEGMENT}/${id}`;
