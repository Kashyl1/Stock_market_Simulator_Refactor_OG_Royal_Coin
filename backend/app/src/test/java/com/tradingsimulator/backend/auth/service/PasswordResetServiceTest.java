package com.tradingsimulator.backend.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.tradingsimulator.backend.auth.AuthError;
import com.tradingsimulator.backend.auth.AuthMailNotifier;
import com.tradingsimulator.backend.auth.TokenType;
import com.tradingsimulator.backend.auth.User;
import com.tradingsimulator.backend.auth.UserRepository;
import com.tradingsimulator.backend.auth.UserStatus;
import com.tradingsimulator.backend.auth.UserToken;
import com.tradingsimulator.backend.auth.password.PasswordPolicy;
import com.tradingsimulator.backend.auth.token.OneTimeTokenService;
import com.tradingsimulator.backend.common.error.AppException;
import com.tradingsimulator.backend.support.TestUsers;

class PasswordResetServiceTest {

	private static final String RAW_TOKEN = "raw-token";
	private static final String TOKEN_HASH = "token-hash";
	private static final String NEW_PASSWORD = "brand-new-password";
	private static final String NEW_PASSWORD_HASH = "brand-new-hash";
	private static final Instant EXPIRES_AT = Instant.parse("2026-09-23T11:00:00Z");

	private final UserRepository users = mock(UserRepository.class);
	private final OneTimeTokenService oneTimeTokens = mock(OneTimeTokenService.class);
	private final PasswordPolicy passwordPolicy = mock(PasswordPolicy.class);
	private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
	private final RefreshTokenService refreshTokens = mock(RefreshTokenService.class);
	private final AuthMailNotifier mailNotifier = mock(AuthMailNotifier.class);

	private final PasswordResetService service = new PasswordResetServiceImpl(users, oneTimeTokens, passwordPolicy,
			passwordEncoder, refreshTokens, mailNotifier);

	@Test
	void issuesAResetTokenAndAsksForTheMail() {
		when(users.findByEmailIgnoreCase(TestUsers.EMAIL)).thenReturn(Optional.of(TestUsers.active()));
		when(oneTimeTokens.issueFor(TestUsers.USER_ID, TokenType.RESET_PASSWORD)).thenReturn(RAW_TOKEN);

		service.requestReset(TestUsers.EMAIL);

		verify(mailNotifier).sendPasswordResetLink(TestUsers.EMAIL, RAW_TOKEN);
	}

	@Test
	void staysSilentForAnEmailNobodyRegistered() {
		when(users.findByEmailIgnoreCase(TestUsers.OTHER_EMAIL)).thenReturn(Optional.empty());

		service.requestReset(TestUsers.OTHER_EMAIL);

		verifyNoInteractions(oneTimeTokens, mailNotifier);
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
		when(consume()).thenReturn(UserToken.oneTime(TestUsers.USER_ID, TokenType.RESET_PASSWORD, TOKEN_HASH, EXPIRES_AT));
	}

	private void assertThatResetFailsWith(AuthError expected) {
		assertThatThrownBy(() -> service.reset(RAW_TOKEN, NEW_PASSWORD))
				.isInstanceOf(AppException.class)
				.extracting(thrown -> ((AppException) thrown).errorCode())
				.isEqualTo(expected);
	}
}
