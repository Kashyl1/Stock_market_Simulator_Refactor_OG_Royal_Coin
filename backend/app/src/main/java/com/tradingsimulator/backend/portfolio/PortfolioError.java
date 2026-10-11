package com.tradingsimulator.backend.portfolio;

import org.springframework.http.HttpStatus;

import com.tradingsimulator.backend.common.error.ErrorCode;

public enum PortfolioError implements ErrorCode {

	NOT_FOUND(HttpStatus.NOT_FOUND, "portfolio {0} was not found"),
	NAME_TAKEN(HttpStatus.CONFLICT, "you already have a portfolio called {0}"),
	AMOUNT_TOO_PRECISE(HttpStatus.BAD_REQUEST, "{0} {1} has more decimal places than {1} allows"),
	TRANSFER_TOO_SMALL(HttpStatus.UNPROCESSABLE_CONTENT, "{0} {1} is too little to arrive as anything after the conversion");

	private static final String CODE_PREFIX = "PORTFOLIO.";

	private final HttpStatus status;
	private final String messageTemplate;

	PortfolioError(HttpStatus status, String messageTemplate) {
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
