package com.tradingsimulator.backend.wallet;

import org.springframework.http.HttpStatus;

import com.tradingsimulator.backend.common.error.ErrorCode;

public enum WalletError implements ErrorCode {

	MAIN_ACCOUNT_MISSING(HttpStatus.INTERNAL_SERVER_ERROR, "user {0} has no main account"),
	PORTFOLIO_WALLET_MISSING(HttpStatus.INTERNAL_SERVER_ERROR, "portfolio {0} has no wallet"),
	INSUFFICIENT_FUNDS(HttpStatus.UNPROCESSABLE_CONTENT, "only {0} {1} is available, {2} {1} is needed");

	private static final String CODE_PREFIX = "WALLET.";

	private final HttpStatus status;
	private final String messageTemplate;

	WalletError(HttpStatus status, String messageTemplate) {
		this.status = status;
		this.messageTemplate = messageTemplate;
	}

	@Override
	public String code() {
		return CODE_PREFIX + name();
	}

	@Override
	public HttpStatus status() {
		return status;
	}

	@Override
	public String messageTemplate() {
		return messageTemplate;
	}
}
