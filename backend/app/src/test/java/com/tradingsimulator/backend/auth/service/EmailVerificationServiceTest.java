package com.tradingsimulator.backend.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.tradingsimulator.backend.auth.AuthError;
import com.tradingsimulator.backend.auth.Role;
import com.tradingsimulator.backend.auth.TokenType;
import com.tradingsimulator.backend.auth.User;
import com.tradingsimulator.backend.auth.UserRepository;
import com.tradingsimulator.backend.auth.UserStatus;
import com.tradingsimulator.backend.auth.UserToken;
import com.tradingsimulator.backend.auth.UserTokenRepository;
import com.tradingsimulator.backend.auth.token.OneTimeTokenService;
import com.tradingsimulator.backend.common.error.AppException;
import com.tradingsimulator.backend.support.TestUsers;

class EmailVerificationServiceTest {

	private static final String RAW_TOKEN = "raw-token";
	private static final String TOKEN_HASH = "token-hash";
	private static final Instant NOW = Instant.parse("2026-09-12T10:00:00Z");
	private static final Duration ONE_HOUR = Duration.ofHours(1);

	private final UserRepository users = mock(UserRepository.class);
	private final UserTokenRepository userTokens = mock(UserTokenRepository.class);
	private final OneTimeTokenService oneTimeTokens = mock(OneTimeTokenService.class);

	private final EmailVerificationService service = new EmailVerificationServiceImpl(users, userTokens,
			oneTimeTokens, Clock.fixed(NOW, ZoneOffset.UTC));

	@BeforeEach
	void stubHashing() {
		when(oneTimeTokens.hash(RAW_TOKEN)).thenReturn(TOKEN_HASH);
	}

	@Test
	void activatesTheUserAndConsumesTheToken() {
		UserToken token = verificationToken(NOW.plus(ONE_HOUR));
		User user = pendingUser();
		givenToken(token);
		when(users.findById(TestUsers.USER_ID)).thenReturn(Optional.of(user));

		service.verify(RAW_TOKEN);

		assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
		assertThat(token.getConsumedAt()).isEqualTo(NOW);
	}

	@Test
	void rejectsAnUnknownToken() {
		when(userTokens.findByTokenHashAndTokenType(TOKEN_HASH, TokenType.VERIFY_EMAIL))
				.thenReturn(Optional.empty());

		assertThatVerifyFailsWith(AuthError.VERIFICATION_TOKEN_INVALID);
	}

	@Test
	void rejectsATokenThatWasAlreadyUsed() {
		UserToken token = verificationToken(NOW.plus(ONE_HOUR));
		token.setConsumedAt(NOW.minus(ONE_HOUR));
		givenToken(token);

		assertThatVerifyFailsWith(AuthError.VERIFICATION_TOKEN_INVALID);
	}

	@Test
	void rejectsAnExpiredToken() {
		givenToken(verificationToken(NOW.minus(ONE_HOUR)));

		assertThatVerifyFailsWith(AuthError.VERIFICATION_TOKEN_EXPIRED);
	}

	@Test
	void rejectsATokenForAUserThatIsNoLongerPending() {
		givenToken(verificationToken(NOW.plus(ONE_HOUR)));
		User blocked = pendingUser();
		blocked.setStatus(UserStatus.BLOCKED);
		when(users.findById(TestUsers.USER_ID)).thenReturn(Optional.of(blocked));

		assertThatVerifyFailsWith(AuthError.VERIFICATION_TOKEN_INVALID);
		assertThat(blocked.getStatus()).isEqualTo(UserStatus.BLOCKED);
	}

	private void givenToken(UserToken token) {
		when(userTokens.findByTokenHashAndTokenType(TOKEN_HASH, TokenType.VERIFY_EMAIL))
				.thenReturn(Optional.of(token));
	}

	private void assertThatVerifyFailsWith(AuthError expected) {
		assertThatThrownBy(() -> service.verify(RAW_TOKEN))
				.isInstanceOf(AppException.class)
				.extracting(thrown -> ((AppException) thrown).errorCode())
				.isEqualTo(expected);
	}

	private static UserToken verificationToken(Instant expiresAt) {
		UserToken token = new UserToken();
		token.setUserId(TestUsers.USER_ID);
		token.setTokenType(TokenType.VERIFY_EMAIL);
		token.setTokenHash(TOKEN_HASH);
		token.setExpiresAt(expiresAt);
		return token;
	}

	private static User pendingUser() {
		User user = new User();
		user.setEmail(TestUsers.EMAIL);
		user.setRole(Role.USER);
		user.setStatus(UserStatus.PENDING_VERIFICATION);
		return user;
	}
}
