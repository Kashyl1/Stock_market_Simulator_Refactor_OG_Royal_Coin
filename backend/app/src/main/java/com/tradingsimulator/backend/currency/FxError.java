package com.tradingsimulator.backend.currency;

import org.springframework.http.HttpStatus;

import com.tradingsimulator.backend.common.error.ErrorCode;

public enum FxError implements ErrorCode {

	UNKNOWN_CURRENCY(HttpStatus.BAD_REQUEST, "currency {0} is not supported"),
	RATE_NOT_AVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "there is no exchange rate for {0} on or before {1}"),
	PROVIDER_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "exchange rates from {0} are not available right now"),
	RATE_MISSING_FROM_SOURCE(HttpStatus.SERVICE_UNAVAILABLE, "{1} published no rate for {0} on {2}");

	private static final String CODE_PREFIX = "FX.";

	private final HttpStatus status;
	private final String messageTemplate;

	FxError(HttpStatus status, String messageTemplate) {
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
