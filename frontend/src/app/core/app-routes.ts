export const AppRoute = {
  Home: '',
  Register: 'register',
  Login: 'login',
  VerifyEmail: 'verify-email',
  ForgotPassword: 'forgot-password',
  ResetPassword: 'reset-password',
  Dashboard: 'dashboard',
} as const;

export type AppRoute = (typeof AppRoute)[keyof typeof AppRoute];

export const AppPath = {
  Home: '/',
  Register: `/${AppRoute.Register}`,
  Login: `/${AppRoute.Login}`,
  VerifyEmail: `/${AppRoute.VerifyEmail}`,
  ForgotPassword: `/${AppRoute.ForgotPassword}`,
  ResetPassword: `/${AppRoute.ResetPassword}`,
  Dashboard: `/${AppRoute.Dashboard}`,
} as const;
