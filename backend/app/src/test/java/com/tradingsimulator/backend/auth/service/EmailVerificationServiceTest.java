package com.tradingsimulator.backend.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.tradingsimulator.backend.auth.AuthError;
import com.tradingsimulator.backend.auth.TokenType;
import com.tradingsimulator.backend.auth.User;
import com.tradingsimulator.backend.auth.UserRepository;
import com.tradingsimulator.backend.auth.UserStatus;
import com.tradingsimulator.backend.auth.UserToken;
import com.tradingsimulator.backend.auth.token.OneTimeTokenService;
import com.tradingsimulator.backend.common.error.AppException;
import com.tradingsimulator.backend.support.TestUsers;

class EmailVerificationServiceTest {

	private static final String RAW_TOKEN = "raw-token";
	private static final String TOKEN_HASH = "token-hash";
	private static final Instant EXPIRES_AT = Instant.parse("2026-09-13T10:00:00Z");

	private final UserRepository users = mock(UserRepository.class);
	private final OneTimeTokenService oneTimeTokens = mock(OneTimeTokenService.class);

	private final EmailVerificationService service = new EmailVerificationServiceImpl(users, oneTimeTokens);

	@Test
	void activatesThePendingUserBehindTheToken() {
		givenToken();
		User user = TestUsers.withStatus(UserStatus.PENDING_VERIFICATION);
		when(users.findById(TestUsers.USER_ID)).thenReturn(Optional.of(user));

		service.verify(RAW_TOKEN);

		assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
	}

	@Test
	void passesOnTheFailureOfAnInvalidToken() {
		when(consume()).thenThrow(new AppException(AuthError.VERIFICATION_TOKEN_INVALID));

		assertThatVerifyFailsWith();
	}

	@Test
	void rejectsATokenWhoseUserIsGone() {
		givenToken();
		when(users.findById(TestUsers.USER_ID)).thenReturn(Optional.empty());

		assertThatVerifyFailsWith();
	}

	@Test
	void rejectsATokenForAUserThatIsNoLongerPending() {
		givenToken();
		User blocked = TestUsers.withStatus(UserStatus.BLOCKED);
		when(users.findById(TestUsers.USER_ID)).thenReturn(Optional.of(blocked));

		assertThatVerifyFailsWith();
		assertThat(blocked.getStatus()).isEqualTo(UserStatus.BLOCKED);
	}

	private UserToken consume() {
		return oneTimeTokens.consume(RAW_TOKEN, TokenType.VERIFY_EMAIL, AuthError.VERIFICATION_TOKEN_INVALID, AuthError.VERIFICATION_TOKEN_EXPIRED);
	}

	private void givenToken() {
		when(consume()).thenReturn(UserToken.oneTime(TestUsers.USER_ID, TokenType.VERIFY_EMAIL, TOKEN_HASH, EXPIRES_AT));
	}

	private void assertThatVerifyFailsWith() {
		assertThatThrownBy(() -> service.verify(RAW_TOKEN)).isInstanceOf(AppException.class).extracting(thrown -> ((AppException) thrown).errorCode())
				.isEqualTo(AuthError.VERIFICATION_TOKEN_INVALID);
	}
}
