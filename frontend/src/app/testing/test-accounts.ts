import { Account } from '../auth/auth.service';
import { Role } from '../core/role';
import { UserStatus } from '../core/user-status';
import { TestUsers } from './test-users';

export const TEST_ACCESS_TOKEN = 'access-token';
export const TEST_REFRESHED_TOKEN = 'refreshed-token';
export const TEST_EXPIRES_IN_SECONDS = 3600;

export const TEST_ACCOUNT: Account = {
  id: 7,
  email: TestUsers.email,
  displayName: TestUsers.displayName,
  role: Role.User,
  status: UserStatus.Active,
};
