package com.tradingsimulator.backend.auth.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.tradingsimulator.backend.auth.AuthError;
import com.tradingsimulator.backend.auth.AuthMailNotifier;
import com.tradingsimulator.backend.auth.TokenType;
import com.tradingsimulator.backend.auth.UserStatus;
import com.tradingsimulator.backend.auth.UserTokenRepository;
import com.tradingsimulator.backend.auth.VerificationResendProperties;
import com.tradingsimulator.backend.auth.token.OneTimeTokenService;
import com.tradingsimulator.backend.common.error.AppException;
import com.tradingsimulator.backend.support.TestUsers;

class VerificationResendServiceTest {

	private static final Instant NOW = Instant.parse("2026-10-10T15:00:00Z");
	private static final Duration COOLDOWN = Duration.ofSeconds(60);
	private static final Duration WINDOW = Duration.ofHours(24);
	private static final int RESEND_LIMIT = 3;
	private static final long LINKS_SO_FAR = 2;
	private static final long LINKS_AT_THE_LIMIT = RESEND_LIMIT + 1;
	private static final String NEW_RAW_TOKEN = "new-raw-token";

	private final CredentialsVerifier credentials = mock(CredentialsVerifier.class);
	private final UserTokenRepository userTokens = mock(UserTokenRepository.class);
	private final OneTimeTokenService oneTimeTokens = mock(OneTimeTokenService.class);
	private final AuthMailNotifier mailNotifier = mock(AuthMailNotifier.class);
	private final VerificationResendProperties properties = new VerificationResendProperties(COOLDOWN, RESEND_LIMIT, WINDOW);
	private final VerificationResendService service = new VerificationResendServiceImpl(credentials, userTokens, oneTimeTokens, mailNotifier, properties,
			Clock.fixed(NOW, ZoneOffset.UTC));

	@BeforeEach
	void givenAPendingAccount() {
		givenStatus(UserStatus.PENDING_VERIFICATION);
		givenLinksInWindow(LINKS_SO_FAR);
		when(oneTimeTokens.reissueFor(TestUsers.USER_ID, TokenType.VERIFY_EMAIL)).thenReturn(NEW_RAW_TOKEN);
	}

	@Test
	void sendsANewLinkToAPendingAccountWithTheRightPassword() {
		service.resend(TestUsers.EMAIL, TestUsers.PASSWORD);

		verify(oneTimeTokens).reissueFor(TestUsers.USER_ID, TokenType.VERIFY_EMAIL);
		verify(mailNotifier).sendVerificationLink(TestUsers.EMAIL, NEW_RAW_TOKEN);
	}

	@Test
	void refusesWrongCredentialsLikeLogin() {
		when(credentials.verify(TestUsers.EMAIL, TestUsers.PASSWORD)).thenThrow(new AppException(AuthError.INVALID_CREDENTIALS));

		assertFailsWith(() -> service.resend(TestUsers.EMAIL, TestUsers.PASSWORD), AuthError.INVALID_CREDENTIALS);
		verifyNoInteractions(mailNotifier);
	}

	@Test
	void tellsAVerifiedAccountToSignIn() {
		givenStatus(UserStatus.ACTIVE);

		assertFailsWith(() -> service.resend(TestUsers.EMAIL, TestUsers.PASSWORD), AuthError.ACCOUNT_ALREADY_VERIFIED);
		verifyNoInteractions(mailNotifier);
	}

	@Test
	void sendsNothingToABlockedAccount() {
		givenStatus(UserStatus.BLOCKED);

		assertFailsWith(() -> service.resend(TestUsers.EMAIL, TestUsers.PASSWORD), AuthError.ACCOUNT_BLOCKED);
		verifyNoInteractions(mailNotifier);
	}

	@Test
	void waitsAMinuteBetweenTwoLinks() {
		when(userTokens.existsByUserIdAndTokenTypeAndCreatedWhenAfter(TestUsers.USER_ID, TokenType.VERIFY_EMAIL, NOW.minus(COOLDOWN))).thenReturn(true);

		assertFailsWith(() -> service.resend(TestUsers.EMAIL, TestUsers.PASSWORD), AuthError.VERIFICATION_RESEND_TOO_SOON);
		verifyNoInteractions(mailNotifier);
	}

	@Test
	void stopsAfterThreeResendsADay() {
		givenLinksInWindow(LINKS_AT_THE_LIMIT);

		assertFailsWith(() -> service.resend(TestUsers.EMAIL, TestUsers.PASSWORD), AuthError.VERIFICATION_RESEND_LIMIT);
		verifyNoInteractions(mailNotifier);
	}

	private void givenLinksInWindow(long links) {
		when(userTokens.countByUserIdAndTokenTypeAndCreatedWhenAfter(TestUsers.USER_ID, TokenType.VERIFY_EMAIL, NOW.minus(WINDOW))).thenReturn(links);
	}

	private void givenStatus(UserStatus status) {
		when(credentials.verify(TestUsers.EMAIL, TestUsers.PASSWORD)).thenReturn(TestUsers.withStatus(status));
	}

	private static void assertFailsWith(ThrowingCallable call, AuthError expected) {
		assertThatThrownBy(call).isInstanceOf(AppException.class).extracting(failure -> ((AppException) failure).errorCode()).isEqualTo(expected);
	}
}
