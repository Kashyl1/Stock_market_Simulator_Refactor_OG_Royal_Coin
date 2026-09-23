package com.tradingsimulator.backend.auth.service;

import com.tradingsimulator.backend.auth.User;

public interface RefreshTokenService {

	AuthTokens issue(User user, ClientDetails client);

	AuthTokens rotate(String rawToken, ClientDetails client);

	void revoke(String rawToken);

	void revokeEverySessionOf(long userId);
}
