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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.tradingsimulator.backend.auth.AuthError;
import com.tradingsimulator.backend.auth.AuthMailNotifier;
import com.tradingsimulator.backend.auth.Role;
import com.tradingsimulator.backend.auth.TokenType;
import com.tradingsimulator.backend.auth.User;
import com.tradingsimulator.backend.auth.UserRepository;
import com.tradingsimulator.backend.auth.UserStatus;
import com.tradingsimulator.backend.auth.password.PasswordPolicy;
import com.tradingsimulator.backend.auth.token.OneTimeTokenService;
import com.tradingsimulator.backend.common.error.AppException;
import com.tradingsimulator.backend.support.TestEntities;
import com.tradingsimulator.backend.support.TestUsers;
import com.tradingsimulator.backend.wallet.Wallet;
import com.tradingsimulator.backend.wallet.WalletRepository;

class RegistrationServiceTest {

	private static final String RAW_TOKEN = "raw-token";

	private final UserRepository users = mock(UserRepository.class);
	private final WalletRepository wallets = mock(WalletRepository.class);
	private final PasswordPolicy passwordPolicy = mock(PasswordPolicy.class);
	private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
	private final OneTimeTokenService oneTimeTokens = mock(OneTimeTokenService.class);
	private final AuthMailNotifier mailNotifier = mock(AuthMailNotifier.class);

	private final RegistrationService service = new RegistrationServiceImpl(users, wallets, passwordPolicy, passwordEncoder, oneTimeTokens, mailNotifier);

	@BeforeEach
	void stubCollaborators() {
		when(passwordEncoder.encode(TestUsers.PASSWORD)).thenReturn(TestUsers.PASSWORD_HASH);
		when(oneTimeTokens.issueFor(TestUsers.USER_ID, TokenType.VERIFY_EMAIL)).thenReturn(RAW_TOKEN);
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
		assertThat(wallet.getValue().getCurrency()).isEqualTo(Wallet.DEFAULT_CURRENCY);
		assertThat(wallet.getValue().getCashBalance()).isEqualByComparingTo(BigDecimal.ZERO);
		assertThat(wallet.getValue().getReservedBalance()).isEqualByComparingTo(BigDecimal.ZERO);

		verify(mailNotifier).sendVerificationLink(TestUsers.EMAIL, RAW_TOKEN);
	}

	@Test
	void storesTheEmailInLowerCase() {
		register(TestUsers.EMAIL_IN_CAPITALS);

		ArgumentCaptor<User> user = ArgumentCaptor.forClass(User.class);
		verify(users).saveAndFlush(user.capture());
		assertThat(user.getValue().getEmail()).isEqualTo(TestUsers.EMAIL);
		verify(mailNotifier).sendVerificationLink(TestUsers.EMAIL, RAW_TOKEN);
	}

	@Test
	void storesNothingWhenThePasswordIsTooWeak() {
		doThrow(new AppException(AuthError.PASSWORD_TOO_WEAK, PasswordPolicy.MIN_LENGTH)).when(passwordPolicy).check(TestUsers.PASSWORD);

		assertThatRegisterFailsWith(AuthError.PASSWORD_TOO_WEAK);

		verifyNoInteractions(users, wallets, oneTimeTokens, mailNotifier);
	}

	@Test
	void rejectsAnEmailThatIsAlreadyRegistered() {
		when(users.existsByEmailIgnoreCase(TestUsers.EMAIL)).thenReturn(true);

		assertThatRegisterFailsWith(AuthError.EMAIL_ALREADY_REGISTERED);

		verify(users, never()).saveAndFlush(any(User.class));
		verifyNoInteractions(wallets, oneTimeTokens, mailNotifier);
	}

	@Test
	void turnsAUniqueViolationIntoTheSameRegisteredEmailError() {
		when(users.saveAndFlush(any(User.class))).thenThrow(new DataIntegrityViolationException("uq_app_user_email"));

		assertThatRegisterFailsWith(AuthError.EMAIL_ALREADY_REGISTERED);

		verifyNoInteractions(wallets, oneTimeTokens, mailNotifier);
	}

	private RegistrationResult register(String email) {
		return service.register(email, TestUsers.PASSWORD, TestUsers.DISPLAY_NAME);
	}

	private void assertThatRegisterFailsWith(AuthError expected) {
		assertThatThrownBy(() -> register(TestUsers.EMAIL))
				.isInstanceOf(AppException.class)
				.extracting(thrown -> ((AppException) thrown).errorCode())
				.isEqualTo(expected);
	}
}
