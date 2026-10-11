package com.tradingsimulator.backend.account.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tradingsimulator.backend.account.service.AccountEntryView;
import com.tradingsimulator.backend.account.service.AccountService;
import com.tradingsimulator.backend.account.service.AccountView;
import com.tradingsimulator.backend.auth.service.CurrentUserService;
import com.tradingsimulator.backend.web.PageResponse;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(AccountPaths.BASE)
@RequiredArgsConstructor
public class AccountController {

	private final AccountService accounts;
	private final CurrentUserService currentUser;

	@GetMapping
	public AccountView account() {
		return accounts.account(currentUser.current().id());
	}

	@GetMapping(AccountPaths.ENTRIES)
	public PageResponse<AccountEntryView> entries(@RequestParam(defaultValue = PageResponse.FIRST_PAGE) @PositiveOrZero int page,
			@RequestParam(defaultValue = PageResponse.DEFAULT_SIZE) @Positive @Max(PageResponse.MAX_SIZE) int size) {
		return accounts.entries(currentUser.current().id(), page, size);
	}
}
