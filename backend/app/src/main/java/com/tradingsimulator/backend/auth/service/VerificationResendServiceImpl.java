package com.tradingsimulator.backend.auth.service;

import java.time.Clock;
import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tradingsimulator.backend.auth.AuthError;
import com.tradingsimulator.backend.auth.AuthMailNotifier;
import com.tradingsimulator.backend.auth.TokenType;
import com.tradingsimulator.backend.auth.User;
import com.tradingsimulator.backend.auth.UserTokenRepository;
import com.tradingsimulator.backend.auth.VerificationResendProperties;
import com.tradingsimulator.backend.auth.token.OneTimeTokenService;
import com.tradingsimulator.backend.common.error.AppException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class VerificationResendServiceImpl implements VerificationResendService {

	private final CredentialsVerifier credentials;
	private final UserTokenRepository userTokens;
	private final OneTimeTokenService oneTimeTokens;
	private final AuthMailNotifier mailNotifier;
	private final VerificationResendProperties properties;
	private final Clock clock;

	@Override
	@Transactional
	public void resend(String email, String password) {
		User user = credentials.verify(email, password);
		switch (user.getStatus()) {
			case ACTIVE -> throw new AppException(AuthError.ACCOUNT_ALREADY_VERIFIED);
			case BLOCKED -> throw new AppException(AuthError.ACCOUNT_BLOCKED);
			case PENDING_VERIFICATION -> requireResendAllowed(user);
		}
		mailNotifier.sendVerificationLink(user.getEmail(), oneTimeTokens.reissueFor(user.getId(), TokenType.VERIFY_EMAIL));
	}

	private void requireResendAllowed(User user) {
		Instant now = clock.instant();
		if (userTokens.existsByUserIdAndTokenTypeAndCreatedWhenAfter(user.getId(), TokenType.VERIFY_EMAIL, now.minus(properties.cooldown()))) {
			throw new AppException(AuthError.VERIFICATION_RESEND_TOO_SOON, properties.cooldown().toSeconds());
		}
		long linksInWindow = userTokens.countByUserIdAndTokenTypeAndCreatedWhenAfter(user.getId(), TokenType.VERIFY_EMAIL, now.minus(properties.window()));
		if (linksInWindow > properties.limit()) {
			throw new AppException(AuthError.VERIFICATION_RESEND_LIMIT);
		}
	}
}
