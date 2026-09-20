export const UserStatus = {
  Active: 'ACTIVE',
  Blocked: 'BLOCKED',
  PendingVerification: 'PENDING_VERIFICATION',
} as const;

export type UserStatus = (typeof UserStatus)[keyof typeof UserStatus];
