package com.tradingsimulator.backend.auth.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.tradingsimulator.backend.auth.AuthError;
import com.tradingsimulator.backend.auth.User;
import com.tradingsimulator.backend.auth.UserRepository;
import com.tradingsimulator.backend.common.error.AppException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CredentialsVerifierImpl implements CredentialsVerifier {

	private final UserRepository users;
	private final PasswordEncoder passwordEncoder;

	@Override
	public User verify(String email, String password) {
		User user = users.findByEmailIgnoreCase(email).orElseThrow(() -> new AppException(AuthError.INVALID_CREDENTIALS));
		if (!passwordEncoder.matches(password, user.getPasswordHash())) {
			throw new AppException(AuthError.INVALID_CREDENTIALS);
		}
		return user;
	}
}
