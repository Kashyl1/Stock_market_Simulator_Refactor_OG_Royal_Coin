package com.tradingsimulator.backend.auth.web;

import java.time.Duration;

import org.springframework.http.ResponseCookie;

import com.tradingsimulator.backend.auth.AuthPaths;

public final class RefreshTokenCookie {

	public static final String NAME = "refresh_token";

	private static final String SAME_SITE = "Strict";
	private static final String EMPTY_VALUE = "";

	public static ResponseCookie of(String rawToken, Duration ttl) {
		return builder(rawToken).maxAge(ttl).build();
	}

	public static ResponseCookie cleared() {
		return builder(EMPTY_VALUE).maxAge(Duration.ZERO).build();
	}

	private static ResponseCookie.ResponseCookieBuilder builder(String value) {
		return ResponseCookie.from(NAME, value).httpOnly(true).secure(true).sameSite(SAME_SITE).path(AuthPaths.BASE);
	}

	private RefreshTokenCookie() { }
}
