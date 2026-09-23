package com.tradingsimulator.backend.auth;

import org.springframework.web.util.UriComponentsBuilder;

public final class AuthLinks {

	private static final String VERIFY_EMAIL_PATH = "/verify-email";
	private static final String RESET_PASSWORD_PATH = "/reset-password";
	private static final String TOKEN_QUERY_PARAM = "token";

	public static String verifyEmail(String frontendBaseUrl, String rawToken) {
		return link(frontendBaseUrl, VERIFY_EMAIL_PATH, rawToken);
	}

	public static String resetPassword(String frontendBaseUrl, String rawToken) {
		return link(frontendBaseUrl, RESET_PASSWORD_PATH, rawToken);
	}

	private static String link(String frontendBaseUrl, String path, String rawToken) {
		return UriComponentsBuilder.fromUriString(frontendBaseUrl).path(path).queryParam(TOKEN_QUERY_PARAM, rawToken).build().toUriString();
	}

	private AuthLinks() { }
}
