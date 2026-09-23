package com.tradingsimulator.backend.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.tradingsimulator.backend.auth.RefreshToken;
import com.tradingsimulator.backend.auth.RefreshTokenRepository;
import com.tradingsimulator.backend.support.TestAuthProperties;
import com.tradingsimulator.backend.support.TestClients;
import com.tradingsimulator.backend.support.TestUsers;

class RefreshTokenFamilyRevokerTest {

	private static final Instant NOW = Instant.parse("2026-09-23T10:00:00Z");
	private static final Instant REVOKED_EARLIER = Instant.parse("2026-09-22T10:00:00Z");
	private static final String TOKEN_HASH = "token-hash";
	private static final UUID FAMILY_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");

	private final RefreshTokenRepository refreshTokens = mock(RefreshTokenRepository.class);

	private final RefreshTokenFamilyRevoker revoker = new RefreshTokenFamilyRevoker(refreshTokens, Clock.fixed(NOW, ZoneOffset.UTC));

	@Test
	void revokesEveryLiveTokenOfTheFamilyAndLeavesTheRestAlone() {
		RefreshToken live = token();
		RefreshToken alreadyRevoked = token();
		alreadyRevoked.setRevokedAt(REVOKED_EARLIER);
		when(refreshTokens.findByFamilyId(FAMILY_ID)).thenReturn(List.of(live, alreadyRevoked));

		revoker.revokeFamily(FAMILY_ID);

		assertThat(live.getRevokedAt()).isEqualTo(NOW);
		assertThat(alreadyRevoked.getRevokedAt()).isEqualTo(REVOKED_EARLIER);
	}

	private static RefreshToken token() {
		return RefreshToken.issued(TestUsers.USER_ID, TOKEN_HASH, FAMILY_ID, NOW,
				NOW.plus(TestAuthProperties.REFRESH_TOKEN_TTL), TestClients.USER_AGENT, TestClients.IP);
	}
}
