package com.tradingsimulator.backend.account.service;

import com.tradingsimulator.backend.web.PageResponse;

public interface AccountService {

	AccountView account(Long userId);

	PageResponse<AccountEntryView> entries(Long userId, int page, int size);
}
