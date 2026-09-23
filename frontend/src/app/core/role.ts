export const Role = {
  User: 'USER',
  Support: 'SUPPORT',
  Admin: 'ADMIN',
} as const;

export type Role = (typeof Role)[keyof typeof Role];
