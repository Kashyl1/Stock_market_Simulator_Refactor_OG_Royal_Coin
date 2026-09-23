package com.tradingsimulator.backend.auth.service;

public record LoginResult(AuthTokens tokens, AuthenticatedUser user) { }
