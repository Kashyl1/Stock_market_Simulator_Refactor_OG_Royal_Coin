package com.tradingsimulator.backend.auth.service;

import java.time.Duration;

import com.tradingsimulator.backend.common.Masking;

public record AuthTokens(String accessToken, Duration accessTokenTtl, String refreshToken, Duration refreshTokenTtl) {

	@Override
	public String toString() {
		return "AuthTokens[accessToken=" + Masking.MASK + ", accessTokenTtl=" + accessTokenTtl + ", refreshToken="
				+ Masking.MASK + ", refreshTokenTtl=" + refreshTokenTtl + "]";
	}
}
