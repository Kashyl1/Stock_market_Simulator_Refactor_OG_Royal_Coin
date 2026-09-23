package com.tradingsimulator.backend.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.tradingsimulator.backend.auth.AuthError;
import com.tradingsimulator.backend.auth.Role;
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
import com.tradingsimulator.backend.mail.VerificationEmailRequested;
import com.tradingsimulator.backend.support.TestAuthProperties;
import com.tradingsimulator.backend.support.TestEntities;
import com.tradingsimulator.backend.support.TestUsers;
import com.tradingsimulator.backend.wallet.Wallet;
import com.tradingsimulator.backend.wallet.WalletJpa;
import com.tradingsimulator.backend.wallet.WalletRepository;

class RegistrationServiceTest {

	private static final String RAW_TOKEN = "raw-token";
	private static final String TOKEN_HASH = "token-hash";
	private static final Instant NOW = Instant.parse("2026-09-12T10:00:00Z");
	private static final String EXPECTED_LINK = TestAuthProperties.FRONTEND_BASE_URL + "/verify-email?token=" + RAW_TOKEN;

	private final UserRepository users = mock(UserRepository.class);
	private final UserTokenRepository userTokens = mock(UserTokenRepository.class);
	private final WalletRepository wallets = mock(WalletRepository.class);
	private final PasswordPolicy passwordPolicy = mock(PasswordPolicy.class);
	private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
	private final OneTimeTokenService oneTimeTokens = mock(OneTimeTokenService.class);
	private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);

	private final RegistrationService service = new RegistrationServiceImpl(users, userTokens, wallets,
			passwordPolicy, passwordEncoder, oneTimeTokens, events, TestAuthProperties.create(), Clock.fixed(NOW, ZoneOffset.UTC));

	@BeforeEach
	void stubCollaborators() {
		when(passwordEncoder.encode(TestUsers.PASSWORD)).thenReturn(TestUsers.PASSWORD_HASH);
		when(oneTimeTokens.issue()).thenReturn(new OneTimeToken(RAW_TOKEN, TOKEN_HASH));
		when(users.saveAndFlush(any(User.class))).thenAnswer(invocation -> TestEntities.withId(invocation.getArgument(0), TestUsers.USER_ID));
	}

	@Test
	void createsAPendingUserWithAnEmptyWalletAndAsksForTheVerificationMail() {
		RegistrationResult result = register(TestUsers.EMAIL);

		assertThat(result).isEqualTo(new RegistrationResult(TestUsers.USER_ID, UserStatus.PENDING_VERIFICATION));

		ArgumentCaptor<User> user = ArgumentCaptor.forClass(User.class);
		verify(users).saveAndFlush(user.capture());
		assertThat(user.getValue().getEmail()).isEqualTo(TestUsers.EMAIL);
		assertThat(user.getValue().getPasswordHash()).isEqualTo(TestUsers.PASSWORD_HASH);
		assertThat(user.getValue().getDisplayName()).isEqualTo(TestUsers.DISPLAY_NAME);
		assertThat(user.getValue().getRole()).isEqualTo(Role.USER);
		assertThat(user.getValue().getStatus()).isEqualTo(UserStatus.PENDING_VERIFICATION);

		ArgumentCaptor<Wallet> wallet = ArgumentCaptor.forClass(Wallet.class);
		verify(wallets).save(wallet.capture());
		assertThat(wallet.getValue().getUserId()).isEqualTo(TestUsers.USER_ID);
		assertThat(wallet.getValue().getCurrency()).isEqualTo(WalletJpa.DEFAULT_CURRENCY);
		assertThat(wallet.getValue().getCashBalance()).isEqualByComparingTo(BigDecimal.ZERO);
		assertThat(wallet.getValue().getReservedBalance()).isEqualByComparingTo(BigDecimal.ZERO);

		ArgumentCaptor<UserToken> token = ArgumentCaptor.forClass(UserToken.class);
		verify(userTokens).save(token.capture());
		assertThat(token.getValue().getUserId()).isEqualTo(TestUsers.USER_ID);
		assertThat(token.getValue().getTokenType()).isEqualTo(TokenType.VERIFY_EMAIL);
		assertThat(token.getValue().getTokenHash()).isEqualTo(TOKEN_HASH);
		assertThat(token.getValue().getExpiresAt()).isEqualTo(NOW.plus(TestAuthProperties.VERIFICATION_TOKEN_TTL));

		verify(events).publishEvent(new VerificationEmailRequested(TestUsers.EMAIL, EXPECTED_LINK));
	}

	@Test
	void storesTheEmailInLowerCase() {
		register(TestUsers.EMAIL_IN_CAPITALS);

		ArgumentCaptor<User> user = ArgumentCaptor.forClass(User.class);
		verify(users).saveAndFlush(user.capture());
		assertThat(user.getValue().getEmail()).isEqualTo(TestUsers.EMAIL);
		verify(events).publishEvent(new VerificationEmailRequested(TestUsers.EMAIL, EXPECTED_LINK));
	}

	@Test
	void storesNothingWhenThePasswordIsTooWeak() {
		doThrow(new AppException(AuthError.PASSWORD_TOO_WEAK, PasswordPolicy.MIN_LENGTH)).when(passwordPolicy).check(TestUsers.PASSWORD);

		assertThatRegisterFailsWith(AuthError.PASSWORD_TOO_WEAK);

		verifyNoInteractions(users, wallets, userTokens, events);
	}

	@Test
	void rejectsAnEmailThatIsAlreadyRegistered() {
		when(users.existsByEmailIgnoreCase(TestUsers.EMAIL)).thenReturn(true);

		assertThatRegisterFailsWith(AuthError.EMAIL_ALREADY_REGISTERED);

		verify(users, never()).saveAndFlush(any(User.class));
		verifyNoInteractions(wallets, userTokens, events);
	}

	@Test
	void turnsAUniqueViolationIntoTheSameRegisteredEmailError() {
		when(users.saveAndFlush(any(User.class))).thenThrow(new DataIntegrityViolationException("uq_app_user_email"));

		assertThatRegisterFailsWith(AuthError.EMAIL_ALREADY_REGISTERED);

		verifyNoInteractions(wallets, userTokens, events);
	}

	private RegistrationResult register(String email) {
		return service.register(email, TestUsers.PASSWORD, TestUsers.DISPLAY_NAME);
	}

	private void assertThatRegisterFailsWith(AuthError expected) {
		assertThatThrownBy(() -> register(TestUsers.EMAIL)).isInstanceOf(AppException.class).extracting(thrown -> ((AppException) thrown).errorCode())
				.isEqualTo(expected);
	}
}
