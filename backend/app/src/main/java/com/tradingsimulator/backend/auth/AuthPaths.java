package com.tradingsimulator.backend.auth;

import java.util.List;

import com.tradingsimulator.backend.web.ApiPaths;

public final class AuthPaths {

	public static final String BASE = ApiPaths.API + "/auth";
	public static final String REGISTER = "/register";
	public static final String VERIFY_EMAIL = "/verify-email";
	public static final String LOGIN = "/login";
	public static final String REFRESH = "/refresh";
	public static final String LOGOUT = "/logout";
	public static final String FORGOT_PASSWORD = "/forgot-password";
	public static final String RESET_PASSWORD = "/reset-password";

	public static final List<String> PUBLIC_ENDPOINTS = List.of(REGISTER, VERIFY_EMAIL, LOGIN, REFRESH, LOGOUT,
			FORGOT_PASSWORD, RESET_PASSWORD).stream()
			.map(path -> BASE + path)
			.toList();

	private AuthPaths() {
	}
}
