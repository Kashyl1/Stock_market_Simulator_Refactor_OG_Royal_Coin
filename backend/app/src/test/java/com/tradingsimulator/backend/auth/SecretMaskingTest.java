package com.tradingsimulator.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import com.tradingsimulator.backend.auth.service.AuthTokens;
import com.tradingsimulator.backend.auth.token.OneTimeToken;
import com.tradingsimulator.backend.auth.web.dto.AccessTokenResponse;
import com.tradingsimulator.backend.auth.web.dto.CurrentUserResponse;
import com.tradingsimulator.backend.auth.web.dto.LoginRequest;
import com.tradingsimulator.backend.auth.web.dto.LoginResponse;
import com.tradingsimulator.backend.auth.web.dto.RegisterRequest;
import com.tradingsimulator.backend.auth.web.dto.ResetPasswordRequest;
import com.tradingsimulator.backend.auth.web.dto.VerifyEmailRequest;
import com.tradingsimulator.backend.common.Masking;
import com.tradingsimulator.backend.support.TestAuthProperties;
import com.tradingsimulator.backend.support.TestUsers;

class SecretMaskingTest {

	private static final String SECRET = "do-not-leak-this";
	private static final String TOKEN_HASH = "token-hash";
	private static final long EXPIRES_IN_SECONDS = 3600L;
	private static final CurrentUserResponse ACCOUNT = new CurrentUserResponse(TestUsers.USER_ID, TestUsers.EMAIL,
			TestUsers.DISPLAY_NAME, Role.USER, UserStatus.ACTIVE);

	@Test
	void keepsPasswordsOutOfRequestToString() {
		assertThatHides(new LoginRequest(TestUsers.EMAIL, SECRET));
		assertThatHides(new RegisterRequest(TestUsers.EMAIL, SECRET, TestUsers.DISPLAY_NAME));
		assertThatHides(new ResetPasswordRequest(SECRET, SECRET));
		assertThatHides(new VerifyEmailRequest(SECRET));
	}

	@Test
	void keepsIssuedTokensOutOfResponseToString() {
		assertThatHides(new LoginResponse(SECRET, EXPIRES_IN_SECONDS, ACCOUNT));
		assertThatHides(new AccessTokenResponse(SECRET, EXPIRES_IN_SECONDS));
		assertThatHides(new AuthTokens(SECRET, Duration.ofMinutes(1), SECRET, Duration.ofDays(1)));
		assertThatHides(new OneTimeToken(SECRET, TOKEN_HASH));
	}

	@Test
	void keepsTheSigningKeysOutOfThePropertiesToString() {
		assertThat(TestAuthProperties.create())
				.hasToString("AuthProperties[accessTokenTtl=" + TestAuthProperties.ACCESS_TOKEN_TTL
						+ ", refreshTokenTtl=" + TestAuthProperties.REFRESH_TOKEN_TTL + ", rsaPrivateKey="
						+ Masking.MASK + ", rsaPublicKey=" + Masking.MASK + ", verificationTokenTtl="
						+ TestAuthProperties.VERIFICATION_TOKEN_TTL + ", resetTokenTtl="
						+ TestAuthProperties.RESET_TOKEN_TTL + ", frontendBaseUrl="
						+ TestAuthProperties.FRONTEND_BASE_URL + "]");
	}

	@Test
	void stillShowsWhatIsSafeToRead() {
		assertThat(new LoginRequest(TestUsers.EMAIL, SECRET)).asString().contains(TestUsers.EMAIL);
		assertThat(new OneTimeToken(SECRET, TOKEN_HASH)).asString().contains(TOKEN_HASH);
	}

	private static void assertThatHides(Object carrier) {
		assertThat(carrier).asString().doesNotContain(SECRET).contains(Masking.MASK);
	}
}
