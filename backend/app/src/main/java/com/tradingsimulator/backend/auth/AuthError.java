package com.tradingsimulator.backend.auth;

import org.springframework.http.HttpStatus;

import com.tradingsimulator.backend.common.error.ErrorCode;

public enum AuthError implements ErrorCode {

	EMAIL_ALREADY_REGISTERED(HttpStatus.CONFLICT, "e-mail {0} is already registered"),
	PASSWORD_TOO_WEAK(HttpStatus.BAD_REQUEST, "the password must be at least {0} characters long and must not be a common one"),
	VERIFICATION_TOKEN_INVALID(HttpStatus.BAD_REQUEST, "the verification token is not valid"),
	VERIFICATION_TOKEN_EXPIRED(HttpStatus.BAD_REQUEST, "the verification token has expired"),
	INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "the e-mail or the password is not correct"),
	ACCOUNT_NOT_VERIFIED(HttpStatus.FORBIDDEN, "verify your e-mail address before signing in"),
	ACCOUNT_BLOCKED(HttpStatus.FORBIDDEN, "this account is blocked"),
	REFRESH_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "the session has expired, sign in again"),
	REFRESH_TOKEN_REUSED(HttpStatus.UNAUTHORIZED, "this session token was already used, so every session of the account was ended"),
	RESET_TOKEN_INVALID(HttpStatus.BAD_REQUEST, "the password reset token is not valid"),
	RESET_TOKEN_EXPIRED(HttpStatus.BAD_REQUEST, "the password reset token has expired");

	private static final String CODE_PREFIX = "AUTH.";

	private final HttpStatus status;
	private final String messageTemplate;

	AuthError(HttpStatus status, String messageTemplate) {
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
