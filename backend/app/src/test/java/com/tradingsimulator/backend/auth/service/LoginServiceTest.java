package com.tradingsimulator.backend.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.tradingsimulator.backend.auth.AuthError;
import com.tradingsimulator.backend.auth.Role;
import com.tradingsimulator.backend.auth.User;
import com.tradingsimulator.backend.auth.UserRepository;
import com.tradingsimulator.backend.auth.UserStatus;
import com.tradingsimulator.backend.common.error.AppException;
import com.tradingsimulator.backend.support.TestAuthProperties;
import com.tradingsimulator.backend.support.TestClients;
import com.tradingsimulator.backend.support.TestUsers;

class LoginServiceTest {

	private static final String ACCESS_TOKEN = "access-token";
	private static final String REFRESH_TOKEN = "refresh-token";
	private static final String WRONG_PASSWORD = "not-the-password";

	private final UserRepository users = mock(UserRepository.class);
	private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
	private final RefreshTokenService refreshTokens = mock(RefreshTokenService.class);

	private final LoginService service = new LoginServiceImpl(users, passwordEncoder, refreshTokens);

	@Test
	void returnsTokensAndTheAccountForTheRightPassword() {
		User user = givenUser(UserStatus.ACTIVE);
		when(passwordEncoder.matches(TestUsers.PASSWORD, TestUsers.PASSWORD_HASH)).thenReturn(true);
		when(refreshTokens.issue(user, TestClients.details())).thenReturn(tokens());

		LoginResult result = login(TestUsers.PASSWORD);

		assertThat(result.tokens().accessToken()).isEqualTo(ACCESS_TOKEN);
		assertThat(result.tokens().refreshToken()).isEqualTo(REFRESH_TOKEN);
		assertThat(result.user()).isEqualTo(new AuthenticatedUser(TestUsers.USER_ID, TestUsers.EMAIL, TestUsers.DISPLAY_NAME, Role.USER, UserStatus.ACTIVE));
	}

	@Test
	void rejectsAnUnknownEmailWithoutSayingSo() {
		when(users.findByEmailIgnoreCase(TestUsers.EMAIL)).thenReturn(Optional.empty());

		assertThatLoginFailsWith(TestUsers.PASSWORD, AuthError.INVALID_CREDENTIALS);

		verifyNoInteractions(refreshTokens);
	}

	@Test
	void rejectsAWrongPasswordWithTheSameError() {
		givenUser(UserStatus.ACTIVE);
		when(passwordEncoder.matches(WRONG_PASSWORD, TestUsers.PASSWORD_HASH)).thenReturn(false);

		assertThatLoginFailsWith(WRONG_PASSWORD, AuthError.INVALID_CREDENTIALS);

		verifyNoInteractions(refreshTokens);
	}

	@Test
	void refusesAnAccountThatWasNeverVerified() {
		givenUser(UserStatus.PENDING_VERIFICATION);
		when(passwordEncoder.matches(TestUsers.PASSWORD, TestUsers.PASSWORD_HASH)).thenReturn(true);

		assertThatLoginFailsWith(TestUsers.PASSWORD, AuthError.ACCOUNT_NOT_VERIFIED);

		verifyNoInteractions(refreshTokens);
	}

	@Test
	void refusesABlockedAccount() {
		givenUser(UserStatus.BLOCKED);
		when(passwordEncoder.matches(TestUsers.PASSWORD, TestUsers.PASSWORD_HASH)).thenReturn(true);

		assertThatLoginFailsWith(TestUsers.PASSWORD, AuthError.ACCOUNT_BLOCKED);

		verifyNoInteractions(refreshTokens);
	}

	private User givenUser(UserStatus status) {
		User user = TestUsers.withStatus(status);
		when(users.findByEmailIgnoreCase(TestUsers.EMAIL)).thenReturn(Optional.of(user));
		return user;
	}

	private LoginResult login(String password) {
		return service.login(TestUsers.EMAIL, password, TestClients.details());
	}

	private void assertThatLoginFailsWith(String password, AuthError expected) {
		assertThatThrownBy(() -> login(password)).isInstanceOf(AppException.class).extracting(thrown -> ((AppException) thrown).errorCode())
				.isEqualTo(expected);
	}

	private static AuthTokens tokens() {
		return new AuthTokens(ACCESS_TOKEN, TestAuthProperties.ACCESS_TOKEN_TTL, REFRESH_TOKEN, TestAuthProperties.REFRESH_TOKEN_TTL);
	}
}
