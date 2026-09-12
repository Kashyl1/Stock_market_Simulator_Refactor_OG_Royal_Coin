package com.tradingsimulator.backend.auth.password;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.tradingsimulator.backend.auth.AuthError;
import com.tradingsimulator.backend.common.error.AppException;

class PasswordPolicyImplTest {

	private static final String STRONG_PASSWORD = "correct-horse-battery";
	private static final String SHORT_PASSWORD = "short1";
	private static final String COMMON_PASSWORD_IN_CAPITALS = "PASSWORD123";
	private static final String TOO_LONG_PASSWORD = "x".repeat(PasswordPolicy.MAX_LENGTH + 1);

	private final PasswordPolicy policy = new PasswordPolicyImpl();

	@Test
	void acceptsALongEnoughUncommonPassword() {
		assertThatCode(() -> policy.check(STRONG_PASSWORD)).doesNotThrowAnyException();
	}

	@Test
	void rejectsAPasswordShorterThanTheMinimum() {
		assertThatThrownBy(() -> policy.check(SHORT_PASSWORD))
				.isInstanceOf(AppException.class)
				.extracting(thrown -> ((AppException) thrown).errorCode())
				.isEqualTo(AuthError.PASSWORD_TOO_WEAK);
	}

	@Test
	void rejectsACommonPasswordRegardlessOfCase() {
		assertThatThrownBy(() -> policy.check(COMMON_PASSWORD_IN_CAPITALS))
				.isInstanceOf(AppException.class)
				.extracting(thrown -> ((AppException) thrown).errorCode())
				.isEqualTo(AuthError.PASSWORD_TOO_WEAK);
	}

	@Test
	void rejectsAPasswordLongerThanBcryptCanHash() {
		assertThatThrownBy(() -> policy.check(TOO_LONG_PASSWORD))
				.isInstanceOf(AppException.class)
				.extracting(thrown -> ((AppException) thrown).errorCode())
				.isEqualTo(AuthError.PASSWORD_TOO_WEAK);
	}
}
