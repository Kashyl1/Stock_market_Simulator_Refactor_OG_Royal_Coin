package com.tradingsimulator.backend.auth;

public enum Role {
	USER,
	SUPPORT,
	ADMIN;

	private static final String AUTHORITY_PREFIX = "ROLE_";

	public String authority() {
		return AUTHORITY_PREFIX + name();
	}
}
