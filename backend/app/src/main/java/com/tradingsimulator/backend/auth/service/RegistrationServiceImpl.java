package com.tradingsimulator.backend.auth.service;

import java.math.BigDecimal;
import java.time.Clock;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import com.tradingsimulator.backend.auth.AuthError;
import com.tradingsimulator.backend.auth.AuthProperties;
import com.tradingsimulator.backend.auth.Role;
import com.tradingsimulator.backend.auth.TokenType;
import com.tradingsimulator.backend.auth.User;
import com.tradingsimulator.backend.auth.UserRepository;
import com.tradingsimulator.backend.auth.UserStatus;
import com.tradingsimulator.backend.auth.UserToken;
import com.tradingsimulator.backend.auth.UserTokenRepository;
import com.tradingsimulator.backend.auth.password.PasswordPolicy;
import com.tradingsimulator.backend.auth.token.OneTimeToken;
import com.tradingsimulator.backend.auth.token.OneTimeTokenService;
import com.tradingsimulator.backend.common.error.AppException;
import com.tradingsimulator.backend.mail.MailSender;
import com.tradingsimulator.backend.wallet.Wallet;
import com.tradingsimulator.backend.wallet.WalletJpa;
import com.tradingsimulator.backend.wallet.WalletRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RegistrationServiceImpl implements RegistrationService {

	private static final String VERIFY_EMAIL_PATH = "/verify-email";
	private static final String TOKEN_QUERY_PARAM = "token";

	private final UserRepository users;
	private final UserTokenRepository userTokens;
	private final WalletRepository wallets;
	private final PasswordPolicy passwordPolicy;
	private final PasswordEncoder passwordEncoder;
	private final OneTimeTokenService oneTimeTokens;
	private final MailSender mailSender;
	private final AuthProperties properties;
	private final Clock clock;

	@Override
	@Transactional
	public RegistrationResult register(String email, String password, String displayName) {
		passwordPolicy.check(password);
		if (users.existsByEmailIgnoreCase(email)) {
			throw new AppException(AuthError.EMAIL_ALREADY_REGISTERED, email);
		}

		User user = saveUser(email, password, displayName);
		wallets.save(emptyWallet(user.getId()));

		OneTimeToken token = oneTimeTokens.issue();
		userTokens.save(verificationToken(user.getId(), token.hash()));
		mailSender.sendVerificationEmail(user.getEmail(), verificationLink(token.raw()));

		return new RegistrationResult(user.getId(), user.getStatus());
	}

	private User saveUser(String email, String password, String displayName) {
		User user = new User();
		user.setEmail(email);
		user.setPasswordHash(passwordEncoder.encode(password));
		user.setDisplayName(displayName);
		user.setRole(Role.USER);
		user.setStatus(UserStatus.PENDING_VERIFICATION);
		try {
			return users.saveAndFlush(user);
		}
		catch (DataIntegrityViolationException takenEmail) {
			throw new AppException(AuthError.EMAIL_ALREADY_REGISTERED, takenEmail, email);
		}
	}

	private static Wallet emptyWallet(Long userId) {
		Wallet wallet = new Wallet();
		wallet.setUserId(userId);
		wallet.setCurrency(WalletJpa.DEFAULT_CURRENCY);
		wallet.setCashBalance(BigDecimal.ZERO);
		wallet.setReservedBalance(BigDecimal.ZERO);
		return wallet;
	}

	private UserToken verificationToken(Long userId, String tokenHash) {
		UserToken token = new UserToken();
		token.setUserId(userId);
		token.setTokenType(TokenType.VERIFY_EMAIL);
		token.setTokenHash(tokenHash);
		token.setExpiresAt(clock.instant().plus(properties.verificationTokenTtl()));
		return token;
	}

	private String verificationLink(String rawToken) {
		return UriComponentsBuilder.fromUriString(properties.frontendBaseUrl())
				.path(VERIFY_EMAIL_PATH)
				.queryParam(TOKEN_QUERY_PARAM, rawToken)
				.build()
				.toUriString();
	}
}
