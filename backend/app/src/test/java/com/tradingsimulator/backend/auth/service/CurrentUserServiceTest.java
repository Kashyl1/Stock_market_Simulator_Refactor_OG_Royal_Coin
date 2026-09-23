package com.tradingsimulator.backend.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import com.tradingsimulator.backend.auth.AuthError;
import com.tradingsimulator.backend.auth.Role;
import com.tradingsimulator.backend.auth.UserRepository;
import com.tradingsimulator.backend.auth.UserStatus;
import com.tradingsimulator.backend.auth.security.ParsedAccessToken;
import com.tradingsimulator.backend.common.error.AppException;
import com.tradingsimulator.backend.common.error.CommonError;
import com.tradingsimulator.backend.common.error.ErrorCode;
import com.tradingsimulator.backend.support.TestUsers;

class CurrentUserServiceTest {

	private final UserRepository users = mock(UserRepository.class);

	private final CurrentUserService service = new CurrentUserServiceImpl(users);

	@AfterEach
	void clearContext() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void returnsTheAccountBehindTheAccessToken() {
		givenAuthenticatedPrincipal();
		when(users.findById(TestUsers.USER_ID)).thenReturn(Optional.of(TestUsers.active()));

		assertThat(service.current()).isEqualTo(new AuthenticatedUser(TestUsers.USER_ID, TestUsers.EMAIL,
				TestUsers.DISPLAY_NAME, Role.USER, UserStatus.ACTIVE));
	}

	@Test
	void refusesWhenNobodyIsAuthenticated() {
		assertThatCurrentFailsWith(CommonError.AUTHENTICATION_REQUIRED);
	}

	@Test
	void refusesWhenTheAccountIsGone() {
		givenAuthenticatedPrincipal();
		when(users.findById(TestUsers.USER_ID)).thenReturn(Optional.empty());

		assertThatCurrentFailsWith(CommonError.AUTHENTICATION_REQUIRED);
	}

	@Test
	void refusesAnAccountBlockedAfterTheTokenWasIssued() {
		givenAuthenticatedPrincipal();
		when(users.findById(TestUsers.USER_ID)).thenReturn(Optional.of(TestUsers.withStatus(UserStatus.BLOCKED)));

		assertThatCurrentFailsWith(AuthError.ACCOUNT_BLOCKED);
	}

	private static void givenAuthenticatedPrincipal() {
		ParsedAccessToken principal = new ParsedAccessToken(TestUsers.USER_ID, TestUsers.EMAIL, Role.USER);
		SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal,
				null, List.of(new SimpleGrantedAuthority(Role.USER.authority()))));
	}

	private void assertThatCurrentFailsWith(ErrorCode expected) {
		assertThatThrownBy(service::current).isInstanceOf(AppException.class).extracting(thrown -> ((AppException) thrown).errorCode()).isEqualTo(expected);
	}
}
