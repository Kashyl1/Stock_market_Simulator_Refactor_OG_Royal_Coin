package com.tradingsimulator.backend.auth.token;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.tradingsimulator.backend.auth.AuthError;
import com.tradingsimulator.backend.auth.TokenType;
import com.tradingsimulator.backend.auth.UserToken;
import com.tradingsimulator.backend.auth.UserTokenRepository;
import com.tradingsimulator.backend.common.error.AppException;
import com.tradingsimulator.backend.common.error.ErrorCode;
import com.tradingsimulator.backend.support.TestUsers;

class OneTimeTokenServiceTest {

	private static final String SEEDED_ALGORITHM = "SHA1PRNG";
	private static final long SEED = 42L;
	private static final String URL_SAFE_TOKEN = "[A-Za-z0-9_-]+";
	private static final String ANY_TOKEN = "any-raw-token";
	private static final String TOKEN_HASH = "token-hash";
	private static final Instant NOW = Instant.parse("2026-09-23T10:00:00Z");
	private static final Duration ONE_HOUR = Duration.ofHours(1);
	private static final ErrorCode INVALID = AuthError.RESET_TOKEN_INVALID;
	private static final ErrorCode EXPIRED = AuthError.RESET_TOKEN_EXPIRED;

	private final UserTokenRepository userTokens = mock(UserTokenRepository.class);

	private final OneTimeTokenService service = newService();

	@Test
	void issuesAUrlSafeTokenTogetherWithItsHash() {
		OneTimeToken token = service.issue();

		assertThat(token.raw()).matches(URL_SAFE_TOKEN);
		assertThat(token.hash()).matches(URL_SAFE_TOKEN).isNotEqualTo(token.raw());
		assertThat(service.hash(token.raw())).isEqualTo(token.hash());
	}

	@Test
	void issuesADifferentTokenEveryTime() {
		assertThat(service.issue().raw()).isNotEqualTo(service.issue().raw());
	}

	@Test
	void hashesTheSameTokenToTheSameValue() {
		assertThat(service.hash(ANY_TOKEN)).isEqualTo(service.hash(ANY_TOKEN));
	}

	@Test
	void takesItsRandomnessFromTheInjectedSource() {
		assertThat(newService().issue()).isEqualTo(newService().issue());
	}

	@Test
	void marksAValidTokenAsConsumed() {
		UserToken stored = givenStoredToken(NOW.plus(ONE_HOUR));

		UserToken consumed = service.consume(ANY_TOKEN, TokenType.RESET_PASSWORD, INVALID, EXPIRED);

		assertThat(consumed).isSameAs(stored);
		assertThat(consumed.getConsumedAt()).isEqualTo(NOW);
	}

	@Test
	void rejectsATokenNobodyIssued() {
		when(userTokens.findByTokenHashAndTokenType(service.hash(ANY_TOKEN), TokenType.RESET_PASSWORD)).thenReturn(Optional.empty());

		assertThatConsumeFailsWith(INVALID);
	}

	@Test
	void rejectsATokenThatWasAlreadyUsed() {
		givenStoredToken(NOW.plus(ONE_HOUR)).setConsumedAt(NOW.minus(ONE_HOUR));

		assertThatConsumeFailsWith(INVALID);
	}

	@Test
	void rejectsAnExpiredToken() {
		givenStoredToken(NOW.minus(ONE_HOUR));

		assertThatConsumeFailsWith(EXPIRED);
	}

	private UserToken givenStoredToken(Instant expiresAt) {
		UserToken stored = UserToken.oneTime(TestUsers.USER_ID, TokenType.RESET_PASSWORD, TOKEN_HASH, expiresAt);
		when(userTokens.findByTokenHashAndTokenType(service.hash(ANY_TOKEN), TokenType.RESET_PASSWORD)).thenReturn(Optional.of(stored));
		return stored;
	}

	private void assertThatConsumeFailsWith(ErrorCode expected) {
		assertThatThrownBy(() -> service.consume(ANY_TOKEN, TokenType.RESET_PASSWORD, INVALID, EXPIRED)).isInstanceOf(AppException.class)
				.extracting(thrown -> ((AppException) thrown).errorCode()).isEqualTo(expected);
	}

	private OneTimeTokenService newService() {
		return new OneTimeTokenServiceImpl(seededRandom(), userTokens, Clock.fixed(NOW, ZoneOffset.UTC));
	}

	private static SecureRandom seededRandom() {
		try {
			SecureRandom random = SecureRandom.getInstance(SEEDED_ALGORITHM);
			random.setSeed(SEED);
			return random;
		}
		catch (NoSuchAlgorithmException unavailable) {
			throw new IllegalStateException(unavailable);
		}
	}
}
