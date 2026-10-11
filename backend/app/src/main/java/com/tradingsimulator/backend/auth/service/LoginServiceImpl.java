package com.tradingsimulator.backend.auth.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tradingsimulator.backend.auth.User;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LoginServiceImpl implements LoginService {

	private final CredentialsVerifier credentials;
	private final RefreshTokenService refreshTokens;

	@Override
	@Transactional
	public LoginResult login(String email, String password, ClientDetails client) {
		User user = credentials.verify(email, password);
		user.getStatus().requireSignInAllowed();

		return new LoginResult(refreshTokens.issue(user, client), AuthenticatedUser.of(user));
	}
}
