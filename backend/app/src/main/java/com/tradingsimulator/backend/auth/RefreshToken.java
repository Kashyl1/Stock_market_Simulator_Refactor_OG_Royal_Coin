package com.tradingsimulator.backend.auth;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "refresh_token")
public class RefreshToken extends RefreshTokenJpa {

			public static RefreshToken issued(Long userId, String tokenHash, UUID familyId, Instant issuedAt, Instant expiresAt, String userAgent, String ip) {
		RefreshToken token = new RefreshToken();
		token.setUserId(userId);
		token.setTokenHash(tokenHash);
		token.setFamilyId(familyId);
		token.setIssuedAt(issuedAt);
		token.setExpiresAt(expiresAt);
		token.setUserAgent(truncate(userAgent, USER_AGENT_MAX_LENGTH));
		token.setIp(truncate(ip, IP_MAX_LENGTH));
		return token;
	}

	public boolean isSpent() {
		return getRevokedAt() != null || getReplacedBy() != null;
	}

	public boolean hasExpiredAt(Instant moment) {
		return getExpiresAt().isBefore(moment);
	}

	private static String truncate(String value, int maxLength) {
		if (value == null || value.length() <= maxLength) {
			return value;
		}
		return value.substring(0, maxLength);
	}
}
