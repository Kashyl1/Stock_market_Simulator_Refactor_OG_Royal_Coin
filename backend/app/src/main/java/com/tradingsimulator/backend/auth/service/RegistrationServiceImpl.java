package com.tradingsimulator.backend.auth.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tradingsimulator.backend.auth.AuthError;
import com.tradingsimulator.backend.auth.AuthMailNotifier;
import com.tradingsimulator.backend.auth.TokenType;
import com.tradingsimulator.backend.auth.User;
import com.tradingsimulator.backend.auth.UserRepository;
import com.tradingsimulator.backend.auth.password.PasswordPolicy;
import com.tradingsimulator.backend.auth.token.OneTimeTokenService;
import com.tradingsimulator.backend.common.error.AppException;
import com.tradingsimulator.backend.wallet.Wallet;
import com.tradingsimulator.backend.wallet.WalletRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RegistrationServiceImpl implements RegistrationService {

	private final UserRepository users;
	private final WalletRepository wallets;
	private final PasswordPolicy passwordPolicy;
	private final PasswordEncoder passwordEncoder;
	private final OneTimeTokenService oneTimeTokens;
	private final AuthMailNotifier mailNotifier;

	@Override
	@Transactional
	public RegistrationResult register(String email, String password, String displayName) {
		passwordPolicy.check(password);
		if (users.existsByEmailIgnoreCase(email)) {
			throw new AppException(AuthError.EMAIL_ALREADY_REGISTERED, email);
		}

		User user = saveUser(email, password, displayName);
		wallets.save(Wallet.empty(user.getId()));

		String rawToken = oneTimeTokens.issueFor(user.getId(), TokenType.VERIFY_EMAIL);
		mailNotifier.sendVerificationLink(user.getEmail(), rawToken);

		return new RegistrationResult(user.getId(), user.getStatus());
	}

	private User saveUser(String email, String password, String displayName) {
		try {
			return users.saveAndFlush(User.pending(email, passwordEncoder.encode(password), displayName));
		}
		catch (DataIntegrityViolationException takenEmail) {
			throw new AppException(AuthError.EMAIL_ALREADY_REGISTERED, takenEmail, email);
		}
	}
}
