package com.tradingsimulator.backend.auth.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tradingsimulator.backend.auth.AuthError;
import com.tradingsimulator.backend.auth.AuthMailNotifier;
import com.tradingsimulator.backend.auth.TokenType;
import com.tradingsimulator.backend.auth.User;
import com.tradingsimulator.backend.auth.UserRepository;
import com.tradingsimulator.backend.auth.UserToken;
import com.tradingsimulator.backend.auth.password.PasswordPolicy;
import com.tradingsimulator.backend.auth.token.OneTimeTokenService;
import com.tradingsimulator.backend.common.error.AppException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PasswordResetServiceImpl implements PasswordResetService {

	private final UserRepository users;
	private final OneTimeTokenService oneTimeTokens;
	private final PasswordPolicy passwordPolicy;
	private final PasswordEncoder passwordEncoder;
	private final RefreshTokenService refreshTokens;
	private final AuthMailNotifier mailNotifier;

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
		String rawToken = oneTimeTokens.issueFor(user.getId(), TokenType.RESET_PASSWORD);
		mailNotifier.sendPasswordResetLink(user.getEmail(), rawToken);
	}
}
