package com.tradingsimulator.backend.auth;

import com.tradingsimulator.backend.common.error.AppException;

public enum UserStatus {
	ACTIVE,
	BLOCKED,
	PENDING_VERIFICATION;

	public void requireSignInAllowed() {
		switch (this) {
			case ACTIVE -> { }
			case PENDING_VERIFICATION -> throw new AppException(AuthError.ACCOUNT_NOT_VERIFIED);
			case BLOCKED -> throw new AppException(AuthError.ACCOUNT_BLOCKED);
		}
	}
}
