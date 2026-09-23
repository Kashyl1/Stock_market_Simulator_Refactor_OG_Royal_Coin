package com.tradingsimulator.backend.auth.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tradingsimulator.backend.auth.AuthError;
import com.tradingsimulator.backend.auth.User;
import com.tradingsimulator.backend.auth.UserRepository;
import com.tradingsimulator.backend.common.error.AppException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LoginServiceImpl implements LoginService {

	private final UserRepository users;
	private final PasswordEncoder passwordEncoder;
	private final RefreshTokenService refreshTokens;

	@Override
	@Transactional
	public LoginResult login(String email, String password, ClientDetails client) {
		User user = users.findByEmailIgnoreCase(email).orElseThrow(() -> new AppException(AuthError.INVALID_CREDENTIALS));
		if (!passwordEncoder.matches(password, user.getPasswordHash())) {
			throw new AppException(AuthError.INVALID_CREDENTIALS);
		}
		user.getStatus().requireSignInAllowed();

		return new LoginResult(refreshTokens.issue(user, client), AuthenticatedUser.of(user));
	}
}
