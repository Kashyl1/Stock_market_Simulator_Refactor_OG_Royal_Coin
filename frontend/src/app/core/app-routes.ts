export const AppRoute = {
  Home: '',
  Register: 'register',
  Login: 'login',
  VerifyEmail: 'verify-email',
  Dashboard: 'dashboard',
} as const;

export type AppRoute = (typeof AppRoute)[keyof typeof AppRoute];

export const AppPath = {
  Home: '/',
  Register: `/${AppRoute.Register}`,
  Login: `/${AppRoute.Login}`,
  VerifyEmail: `/${AppRoute.VerifyEmail}`,
  Dashboard: `/${AppRoute.Dashboard}`,
} as const;
