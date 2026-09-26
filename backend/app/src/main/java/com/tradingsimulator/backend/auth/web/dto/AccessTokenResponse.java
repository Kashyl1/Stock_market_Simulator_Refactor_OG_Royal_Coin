package com.tradingsimulator.backend.auth.web.dto;

import com.tradingsimulator.backend.common.Masking;

public record AccessTokenResponse(String accessToken, long expiresInSeconds) {

	@Override
	public String toString() {
		return "AccessTokenResponse[accessToken=" + Masking.MASK + ", expiresInSeconds=" + expiresInSeconds + "]";
	}
}
