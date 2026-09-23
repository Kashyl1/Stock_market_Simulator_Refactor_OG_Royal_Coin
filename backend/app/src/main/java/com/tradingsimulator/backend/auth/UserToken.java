package com.tradingsimulator.backend.auth;

import java.time.Instant;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "user_token")
public class UserToken extends UserTokenJpa {

	public static UserToken oneTime(Long userId, TokenType tokenType, String tokenHash, Instant expiresAt) {
		UserToken token = new UserToken();
		token.setUserId(userId);
		token.setTokenType(tokenType);
		token.setTokenHash(tokenHash);
		token.setExpiresAt(expiresAt);
		return token;
	}
}
