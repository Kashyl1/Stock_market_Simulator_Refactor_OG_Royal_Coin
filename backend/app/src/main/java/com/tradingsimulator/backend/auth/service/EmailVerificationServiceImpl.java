package com.tradingsimulator.backend.auth.service;

import java.time.Clock;
import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tradingsimulator.backend.auth.AuthError;
import com.tradingsimulator.backend.auth.TokenType;
import com.tradingsimulator.backend.auth.User;
import com.tradingsimulator.backend.auth.UserRepository;
import com.tradingsimulator.backend.auth.UserStatus;
import com.tradingsimulator.backend.auth.UserToken;
import com.tradingsimulator.backend.auth.UserTokenRepository;
import com.tradingsimulator.backend.auth.token.OneTimeTokenService;
import com.tradingsimulator.backend.common.error.AppException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailVerificationServiceImpl implements EmailVerificationService {

	private final UserRepository users;
	private final UserTokenRepository userTokens;
	private final OneTimeTokenService oneTimeTokens;
	private final Clock clock;

	@Override
	@Transactional
	public void verify(String rawToken) {
		UserToken token = userTokens
				.findByTokenHashAndTokenType(oneTimeTokens.hash(rawToken), TokenType.VERIFY_EMAIL)
				.orElseThrow(() -> new AppException(AuthError.VERIFICATION_TOKEN_INVALID));
		if (token.getConsumedAt() != null) {
			throw new AppException(AuthError.VERIFICATION_TOKEN_INVALID);
		}

		Instant now = clock.instant();
		if (token.getExpiresAt().isBefore(now)) {
			throw new AppException(AuthError.VERIFICATION_TOKEN_EXPIRED);
		}

		User user = users.findById(token.getUserId())
				.orElseThrow(() -> new AppException(AuthError.VERIFICATION_TOKEN_INVALID));
		if (user.getStatus() != UserStatus.PENDING_VERIFICATION) {
			throw new AppException(AuthError.VERIFICATION_TOKEN_INVALID);
		}

		user.setStatus(UserStatus.ACTIVE);
		token.setConsumedAt(now);
	}
}
