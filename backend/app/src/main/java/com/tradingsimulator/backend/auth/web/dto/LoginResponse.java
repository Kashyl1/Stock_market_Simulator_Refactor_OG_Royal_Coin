package com.tradingsimulator.backend.auth.web.dto;

import com.tradingsimulator.backend.common.Masking;

public record LoginResponse(String accessToken, long expiresInSeconds, CurrentUserResponse user) {

	@Override
	public String toString() {
		return "LoginResponse[accessToken=" + Masking.MASK + ", expiresInSeconds=" + expiresInSeconds + ", user=" + user + "]";
	}
}
