import { HttpResourceRef, httpResource } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { environment } from '../../environments/environment';
import { FIRST_PAGE, PageResponse } from '../core/page-response';
import { AccountEntryView, AccountView } from './account-models';

export const ACCOUNT_PATH = '/account';
export const ENTRIES_PATH = '/entries';
export const RECENT_ENTRIES = 10;

export const AccountParam = {
  Page: 'page',
  Size: 'size',
} as const;

@Injectable({ providedIn: 'root' })
export class AccountService {
  private readonly baseUrl = `${environment.apiUrl}${ACCOUNT_PATH}`;

  account(): HttpResourceRef<AccountView | undefined> {
    return httpResource<AccountView>(() => this.baseUrl);
  }

  recentEntries(): HttpResourceRef<PageResponse<AccountEntryView> | undefined> {
    return httpResource<PageResponse<AccountEntryView>>(() => ({
      url: `${this.baseUrl}${ENTRIES_PATH}`,
      params: { [AccountParam.Page]: FIRST_PAGE, [AccountParam.Size]: RECENT_ENTRIES },
    }));
  }
}
