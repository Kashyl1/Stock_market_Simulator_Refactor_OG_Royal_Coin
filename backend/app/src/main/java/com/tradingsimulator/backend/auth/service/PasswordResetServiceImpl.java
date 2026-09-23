package com.tradingsimulator.backend.auth.service;

import java.time.Clock;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tradingsimulator.backend.auth.AuthError;
import com.tradingsimulator.backend.auth.AuthLinks;
import com.tradingsimulator.backend.auth.AuthProperties;
import com.tradingsimulator.backend.auth.TokenType;
import com.tradingsimulator.backend.auth.User;
import com.tradingsimulator.backend.auth.UserRepository;
import com.tradingsimulator.backend.auth.UserToken;
import com.tradingsimulator.backend.auth.UserTokenRepository;
import com.tradingsimulator.backend.auth.password.PasswordPolicy;
import com.tradingsimulator.backend.auth.token.OneTimeToken;
import com.tradingsimulator.backend.auth.token.OneTimeTokenService;
import com.tradingsimulator.backend.common.error.AppException;
import com.tradingsimulator.backend.mail.PasswordResetEmailRequested;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PasswordResetServiceImpl implements PasswordResetService {

	private final UserRepository users;
	private final UserTokenRepository userTokens;
	private final OneTimeTokenService oneTimeTokens;
	private final PasswordPolicy passwordPolicy;
	private final PasswordEncoder passwordEncoder;
	private final RefreshTokenService refreshTokens;
	private final ApplicationEventPublisher events;
	private final AuthProperties properties;
	private final Clock clock;

	@Override
	@Transactional
	public void requestReset(String email) {
		users.findByEmailIgnoreCase(email).ifPresent(this::sendResetLink);
	}

	@Override
	@Transactional
	public void reset(String rawToken, String newPassword) {
		passwordPolicy.check(newPassword);

		UserToken token = oneTimeTokens.consume(rawToken, TokenType.RESET_PASSWORD, AuthError.RESET_TOKEN_INVALID, AuthError.RESET_TOKEN_EXPIRED);
		User user = users.findById(token.getUserId()).orElseThrow(() -> new AppException(AuthError.RESET_TOKEN_INVALID));

		user.setPasswordHash(passwordEncoder.encode(newPassword));
		refreshTokens.revokeEverySessionOf(user.getId());
	}

	private void sendResetLink(User user) {
		OneTimeToken token = oneTimeTokens.issue();
		userTokens.save(UserToken.oneTime(user.getId(), TokenType.RESET_PASSWORD, token.hash(), clock.instant().plus(properties.resetTokenTtl())));
		events.publishEvent(new PasswordResetEmailRequested(user.getEmail(), AuthLinks.resetPassword(properties.frontendBaseUrl(), token.raw())));
	}
}
