package com.tradingsimulator.backend.support;

import java.time.Duration;

import com.tradingsimulator.backend.auth.AuthProperties;

public final class TestAuthProperties {

	public static final Duration ACCESS_TOKEN_TTL = Duration.ofMinutes(60);
	public static final Duration REFRESH_TOKEN_TTL = Duration.ofDays(14);
	public static final Duration VERIFICATION_TOKEN_TTL = Duration.ofHours(24);
	public static final Duration RESET_TOKEN_TTL = Duration.ofHours(1);
	public static final String FRONTEND_BASE_URL = "http://localhost:4200";

	public static AuthProperties create() {
		return new AuthProperties(ACCESS_TOKEN_TTL, REFRESH_TOKEN_TTL, null, null, VERIFICATION_TOKEN_TTL, RESET_TOKEN_TTL, FRONTEND_BASE_URL);
	}

	private TestAuthProperties() { }
}
