export const AppRoute = {
  Home: '',
  Register: 'register',
  Login: 'login',
} as const;

export type AppRoute = (typeof AppRoute)[keyof typeof AppRoute];

export const AppPath = {
  Home: '/',
  Register: `/${AppRoute.Register}`,
  Login: `/${AppRoute.Login}`,
} as const;
