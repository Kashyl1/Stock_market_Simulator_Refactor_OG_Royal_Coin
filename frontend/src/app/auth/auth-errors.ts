export const AuthErrorCode = {
  AccountNotVerified: 'AUTH.ACCOUNT_NOT_VERIFIED',
} as const;

export type AuthErrorCode = (typeof AuthErrorCode)[keyof typeof AuthErrorCode];
