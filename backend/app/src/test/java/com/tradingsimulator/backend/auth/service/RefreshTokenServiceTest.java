package com.tradingsimulator.backend.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.tradingsimulator.backend.auth.AuthError;
import com.tradingsimulator.backend.auth.RefreshToken;
import com.tradingsimulator.backend.auth.RefreshTokenRepository;
import com.tradingsimulator.backend.auth.Role;
import com.tradingsimulator.backend.auth.User;
import com.tradingsimulator.backend.auth.UserRepository;
import com.tradingsimulator.backend.auth.UserStatus;
import com.tradingsimulator.backend.auth.security.JwtService;
import com.tradingsimulator.backend.auth.token.OneTimeToken;
import com.tradingsimulator.backend.auth.token.OneTimeTokenService;
import com.tradingsimulator.backend.common.error.AppException;
import com.tradingsimulator.backend.support.TestAuthProperties;
import com.tradingsimulator.backend.support.TestClients;
import com.tradingsimulator.backend.support.TestEntities;
import com.tradingsimulator.backend.support.TestUsers;

class RefreshTokenServiceTest {

	private static final Instant NOW = Instant.parse("2026-09-23T10:00:00Z");
	private static final Instant LATER = NOW.plus(TestAuthProperties.REFRESH_TOKEN_TTL);
	private static final String ACCESS_TOKEN = "access-token";
	private static final String PRESENTED_RAW = "presented-raw";
	private static final String PRESENTED_HASH = "presented-hash";
	private static final String ISSUED_RAW = "issued-raw";
	private static final String ISSUED_HASH = "issued-hash";
	private static final long PRESENTED_ID = 100L;
	private static final long SUCCESSOR_ID = 101L;
	private static final UUID FAMILY_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");

	private final RefreshTokenRepository refreshTokens = mock(RefreshTokenRepository.class);
	private final RefreshTokenFamilyRevoker familyRevoker = mock(RefreshTokenFamilyRevoker.class);
	private final UserRepository users = mock(UserRepository.class);
	private final JwtService jwtService = mock(JwtService.class);
	private final OneTimeTokenService oneTimeTokens = mock(OneTimeTokenService.class);

	private final RefreshTokenService service = new RefreshTokenServiceImpl(refreshTokens, familyRevoker, users, jwtService,
			oneTimeTokens, TestAuthProperties.create(), Clock.fixed(NOW, ZoneOffset.UTC));

	@BeforeEach
	void stubCollaborators() {
		when(oneTimeTokens.issue()).thenReturn(new OneTimeToken(ISSUED_RAW, ISSUED_HASH));
		when(oneTimeTokens.hash(PRESENTED_RAW)).thenReturn(PRESENTED_HASH);
		when(jwtService.issueAccessToken(TestUsers.USER_ID, TestUsers.EMAIL, Role.USER)).thenReturn(ACCESS_TOKEN);
		when(refreshTokens.saveAndFlush(any(RefreshToken.class))).thenAnswer(invocation -> TestEntities.withId(invocation.getArgument(0), SUCCESSOR_ID));
	}

	@Test
	void opensANewFamilyWhenASessionStarts() {
		AuthTokens tokens = service.issue(TestUsers.active(), TestClients.details());

		assertThat(tokens.accessToken()).isEqualTo(ACCESS_TOKEN);
		assertThat(tokens.refreshToken()).isEqualTo(ISSUED_RAW);

		RefreshToken saved = savedToken();
		assertThat(saved.getUserId()).isEqualTo(TestUsers.USER_ID);
		assertThat(saved.getTokenHash()).isEqualTo(ISSUED_HASH);
		assertThat(saved.getIssuedAt()).isEqualTo(NOW);
		assertThat(saved.getExpiresAt()).isEqualTo(LATER);
		assertThat(saved.getUserAgent()).isEqualTo(TestClients.USER_AGENT);
		assertThat(saved.getIp()).isEqualTo(TestClients.IP);
	}

	@Test
	void rotatesTheTokenAndKeepsTheFamily() {
		RefreshToken presented = givenPresentedToken(LATER);
		givenUser(UserStatus.ACTIVE);

		AuthTokens tokens = service.rotate(PRESENTED_RAW, TestClients.details());

		assertThat(tokens.refreshToken()).isEqualTo(ISSUED_RAW);
		assertThat(savedToken().getFamilyId()).isEqualTo(FAMILY_ID);
		assertThat(presented.getReplacedBy()).isEqualTo(SUCCESSOR_ID);
		assertThat(presented.getRevokedAt()).isEqualTo(NOW);
	}

	@Test
	void killsTheWholeFamilyWhenASpentTokenComesBack() {
		RefreshToken presented = givenPresentedToken(LATER);
		presented.setReplacedBy(SUCCESSOR_ID);

		assertThatRotateFailsWith(AuthError.REFRESH_TOKEN_REUSED);

		verify(familyRevoker).revokeFamily(FAMILY_ID);
		verify(refreshTokens, never()).saveAndFlush(any(RefreshToken.class));
	}

	@Test
	void rejectsAnExpiredToken() {
		givenPresentedToken(NOW.minusSeconds(1));

		assertThatRotateFailsWith(AuthError.REFRESH_TOKEN_INVALID);
	}

	@Test
	void rejectsAnUnknownToken() {
		when(refreshTokens.findByTokenHash(PRESENTED_HASH)).thenReturn(Optional.empty());

		assertThatRotateFailsWith(AuthError.REFRESH_TOKEN_INVALID);
	}

	@Test
	void stopsRotatingForAnAccountThatWasBlockedMeanwhile() {
		givenPresentedToken(LATER);
		givenUser(UserStatus.BLOCKED);

		assertThatRotateFailsWith(AuthError.ACCOUNT_BLOCKED);
	}

	@Test
	void revokesTheWholeFamilyOnLogout() {
		givenPresentedToken(LATER);

		service.revoke(PRESENTED_RAW);

		verify(familyRevoker).revokeFamily(FAMILY_ID);
	}

	@Test
	void revokesEveryLiveSessionOfAUser() {
		RefreshToken first = tokenOfFamily(LATER);
		RefreshToken second = tokenOfFamily(LATER);
		when(refreshTokens.findByUserIdAndRevokedAtIsNull(TestUsers.USER_ID)).thenReturn(List.of(first, second));

		service.revokeEverySessionOf(TestUsers.USER_ID);

		assertThat(first.getRevokedAt()).isEqualTo(NOW);
		assertThat(second.getRevokedAt()).isEqualTo(NOW);
	}

	private RefreshToken givenPresentedToken(Instant expiresAt) {
		RefreshToken presented = TestEntities.withId(tokenOfFamily(expiresAt), PRESENTED_ID);
		when(refreshTokens.findByTokenHash(PRESENTED_HASH)).thenReturn(Optional.of(presented));
		return presented;
	}

	private void givenUser(UserStatus status) {
		User user = TestUsers.withStatus(status);
		when(users.findById(TestUsers.USER_ID)).thenReturn(Optional.of(user));
	}

	private RefreshToken savedToken() {
		ArgumentCaptor<RefreshToken> saved = ArgumentCaptor.forClass(RefreshToken.class);
		verify(refreshTokens).saveAndFlush(saved.capture());
		return saved.getValue();
	}

	private void assertThatRotateFailsWith(AuthError expected) {
		assertThatThrownBy(() -> service.rotate(PRESENTED_RAW, TestClients.details())).isInstanceOf(AppException.class)
				.extracting(thrown -> ((AppException) thrown).errorCode()).isEqualTo(expected);
	}

	private static RefreshToken tokenOfFamily(Instant expiresAt) {
		return RefreshToken.issued(TestUsers.USER_ID, PRESENTED_HASH, FAMILY_ID, NOW, expiresAt, TestClients.USER_AGENT, TestClients.IP);
	}
}
