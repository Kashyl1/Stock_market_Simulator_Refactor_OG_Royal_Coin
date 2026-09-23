package com.tradingsimulator.backend.auth.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tradingsimulator.backend.auth.AuthError;
import com.tradingsimulator.backend.auth.TokenType;
import com.tradingsimulator.backend.auth.User;
import com.tradingsimulator.backend.auth.UserRepository;
import com.tradingsimulator.backend.auth.UserStatus;
import com.tradingsimulator.backend.auth.UserToken;
import com.tradingsimulator.backend.auth.token.OneTimeTokenService;
import com.tradingsimulator.backend.common.error.AppException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailVerificationServiceImpl implements EmailVerificationService {

	private final UserRepository users;
	private final OneTimeTokenService oneTimeTokens;

	@Override
	@Transactional
	public void verify(String rawToken) {
		UserToken token = oneTimeTokens.consume(rawToken, TokenType.VERIFY_EMAIL, AuthError.VERIFICATION_TOKEN_INVALID, AuthError.VERIFICATION_TOKEN_EXPIRED);

		User user = users.findById(token.getUserId()).orElseThrow(() -> new AppException(AuthError.VERIFICATION_TOKEN_INVALID));
		if (user.getStatus() != UserStatus.PENDING_VERIFICATION) {
			throw new AppException(AuthError.VERIFICATION_TOKEN_INVALID);
		}

		user.setStatus(UserStatus.ACTIVE);
	}
}
