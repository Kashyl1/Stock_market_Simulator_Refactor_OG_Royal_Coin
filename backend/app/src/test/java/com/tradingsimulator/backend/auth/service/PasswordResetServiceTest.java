package com.tradingsimulator.backend.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.tradingsimulator.backend.auth.AuthError;
import com.tradingsimulator.backend.auth.TokenType;
import com.tradingsimulator.backend.auth.User;
import com.tradingsimulator.backend.auth.UserRepository;
import com.tradingsimulator.backend.auth.UserStatus;
import com.tradingsimulator.backend.auth.UserToken;
import com.tradingsimulator.backend.auth.UserTokenRepository;
import com.tradingsimulator.backend.auth.password.PasswordPolicy;
import com.tradingsimulator.backend.auth.token.OneTimeToken;
import com.tradingsimulator.backend.auth.token.OneTimeTokenService;
import com.tradingsimulator.backend.common.error.AppException;
import com.tradingsimulator.backend.mail.PasswordResetEmailRequested;
import com.tradingsimulator.backend.support.TestAuthProperties;
import com.tradingsimulator.backend.support.TestUsers;

class PasswordResetServiceTest {

	private static final Instant NOW = Instant.parse("2026-09-23T10:00:00Z");
	private static final String RAW_TOKEN = "raw-token";
	private static final String TOKEN_HASH = "token-hash";
	private static final String NEW_PASSWORD = "brand-new-password";
	private static final String NEW_PASSWORD_HASH = "brand-new-hash";
	private static final String EXPECTED_LINK = TestAuthProperties.FRONTEND_BASE_URL + "/reset-password?token=" + RAW_TOKEN;

	private final UserRepository users = mock(UserRepository.class);
	private final UserTokenRepository userTokens = mock(UserTokenRepository.class);
	private final OneTimeTokenService oneTimeTokens = mock(OneTimeTokenService.class);
	private final PasswordPolicy passwordPolicy = mock(PasswordPolicy.class);
	private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
	private final RefreshTokenService refreshTokens = mock(RefreshTokenService.class);
	private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);

	private final PasswordResetService service = new PasswordResetServiceImpl(users, userTokens, oneTimeTokens,
			passwordPolicy, passwordEncoder, refreshTokens, events, TestAuthProperties.create(), Clock.fixed(NOW, ZoneOffset.UTC));

	@Test
	void storesAResetTokenAndAsksForTheMail() {
		when(users.findByEmailIgnoreCase(TestUsers.EMAIL)).thenReturn(Optional.of(TestUsers.active()));
		when(oneTimeTokens.issue()).thenReturn(new OneTimeToken(RAW_TOKEN, TOKEN_HASH));

		service.requestReset(TestUsers.EMAIL);

		ArgumentCaptor<UserToken> token = ArgumentCaptor.forClass(UserToken.class);
		verify(userTokens).save(token.capture());
		assertThat(token.getValue().getUserId()).isEqualTo(TestUsers.USER_ID);
		assertThat(token.getValue().getTokenType()).isEqualTo(TokenType.RESET_PASSWORD);
		assertThat(token.getValue().getTokenHash()).isEqualTo(TOKEN_HASH);
		assertThat(token.getValue().getExpiresAt()).isEqualTo(NOW.plus(TestAuthProperties.RESET_TOKEN_TTL));

		verify(events).publishEvent(new PasswordResetEmailRequested(TestUsers.EMAIL, EXPECTED_LINK));
	}

	@Test
	void staysSilentForAnEmailNobodyRegistered() {
		when(users.findByEmailIgnoreCase(TestUsers.OTHER_EMAIL)).thenReturn(Optional.empty());

		service.requestReset(TestUsers.OTHER_EMAIL);

		verifyNoInteractions(userTokens, oneTimeTokens, events);
	}

	@Test
	void setsTheNewPasswordAndEndsEverySession() {
		User user = TestUsers.withStatus(UserStatus.ACTIVE);
		givenConsumableToken();
		when(users.findById(TestUsers.USER_ID)).thenReturn(Optional.of(user));
		when(passwordEncoder.encode(NEW_PASSWORD)).thenReturn(NEW_PASSWORD_HASH);

		service.reset(RAW_TOKEN, NEW_PASSWORD);

		assertThat(user.getPasswordHash()).isEqualTo(NEW_PASSWORD_HASH);
		verify(refreshTokens).revokeEverySessionOf(TestUsers.USER_ID);
	}

	@Test
	void refusesAWeakNewPasswordBeforeTouchingTheToken() {
		doThrow(new AppException(AuthError.PASSWORD_TOO_WEAK, PasswordPolicy.MIN_LENGTH)).when(passwordPolicy).check(NEW_PASSWORD);

		assertThatResetFailsWith(AuthError.PASSWORD_TOO_WEAK);

		verifyNoInteractions(oneTimeTokens, refreshTokens);
	}

	@Test
	void passesOnTheFailureOfAnInvalidToken() {
		when(consume()).thenThrow(new AppException(AuthError.RESET_TOKEN_INVALID));

		assertThatResetFailsWith(AuthError.RESET_TOKEN_INVALID);

		verifyNoInteractions(refreshTokens);
	}

	@Test
	void rejectsATokenWhoseUserIsGone() {
		givenConsumableToken();
		when(users.findById(TestUsers.USER_ID)).thenReturn(Optional.empty());

		assertThatResetFailsWith(AuthError.RESET_TOKEN_INVALID);

		verifyNoInteractions(refreshTokens);
	}

	private UserToken consume() {
		return oneTimeTokens.consume(RAW_TOKEN, TokenType.RESET_PASSWORD, AuthError.RESET_TOKEN_INVALID, AuthError.RESET_TOKEN_EXPIRED);
	}

	private void givenConsumableToken() {
		when(consume()).thenReturn(UserToken.oneTime(TestUsers.USER_ID, TokenType.RESET_PASSWORD, TOKEN_HASH, NOW.plus(TestAuthProperties.RESET_TOKEN_TTL)));
	}

	private void assertThatResetFailsWith(AuthError expected) {
		assertThatThrownBy(() -> service.reset(RAW_TOKEN, NEW_PASSWORD)).isInstanceOf(AppException.class)
				.extracting(thrown -> ((AppException) thrown).errorCode()).isEqualTo(expected);
	}
}
