package com.tradingsimulator.backend.auth.service;

import java.time.Duration;

public record AuthTokens(String accessToken, Duration accessTokenTtl, String refreshToken, Duration refreshTokenTtl) { }
