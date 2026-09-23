package com.tradingsimulator.backend.auth.web.dto;

public record AccessTokenResponse(String accessToken, long expiresInSeconds) { }
