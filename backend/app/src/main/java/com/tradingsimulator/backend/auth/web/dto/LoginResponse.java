package com.tradingsimulator.backend.auth.web.dto;

public record LoginResponse(String accessToken, long expiresInSeconds, CurrentUserResponse user) { }
