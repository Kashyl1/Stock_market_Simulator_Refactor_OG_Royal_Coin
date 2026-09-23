package com.tradingsimulator.backend.auth.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tradingsimulator.backend.auth.AuthError;
import com.tradingsimulator.backend.auth.AuthProperties;
import com.tradingsimulator.backend.auth.RefreshToken;
import com.tradingsimulator.backend.auth.RefreshTokenRepository;
import com.tradingsimulator.backend.auth.User;
import com.tradingsimulator.backend.auth.UserRepository;
import com.tradingsimulator.backend.auth.security.JwtService;
import com.tradingsimulator.backend.auth.token.OneTimeToken;
import com.tradingsimulator.backend.auth.token.OneTimeTokenService;
import com.tradingsimulator.backend.common.error.AppException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {

	private final RefreshTokenRepository refreshTokens;
	private final RefreshTokenFamilyRevoker familyRevoker;
	private final UserRepository users;
	private final JwtService jwtService;
	private final OneTimeTokenService oneTimeTokens;
	private final AuthProperties properties;
	private final Clock clock;

	@Override
	@Transactional
	public AuthTokens issue(User user, ClientDetails client) {
		OneTimeToken token = oneTimeTokens.issue();
		save(user.getId(), token.hash(), UUID.randomUUID(), client);
		return tokens(user, token.raw());
	}

	@Override
	@Transactional
	public AuthTokens rotate(String rawToken, ClientDetails client) {
		RefreshToken presented = present(rawToken);
		if (presented.isSpent()) {
			familyRevoker.revokeFamily(presented.getFamilyId());
			throw new AppException(AuthError.REFRESH_TOKEN_REUSED);
		}
		if (presented.hasExpiredAt(clock.instant())) {
			throw new AppException(AuthError.REFRESH_TOKEN_INVALID);
		}

		User user = users.findById(presented.getUserId()).orElseThrow(() -> new AppException(AuthError.REFRESH_TOKEN_INVALID));
		user.getStatus().requireSignInAllowed();

		OneTimeToken next = oneTimeTokens.issue();
		RefreshToken successor = save(user.getId(), next.hash(), presented.getFamilyId(), client);
		presented.setReplacedBy(successor.getId());
		presented.setRevokedAt(clock.instant());
		return tokens(user, next.raw());
	}

	@Override
	@Transactional
	public void revoke(String rawToken) {
		refreshTokens.findByTokenHash(oneTimeTokens.hash(rawToken)).ifPresent(token -> familyRevoker.revokeFamily(token.getFamilyId()));
	}

	@Override
	@Transactional
	public void revokeEverySessionOf(long userId) {
		revokeAll(refreshTokens.findByUserIdAndRevokedAtIsNull(userId));
	}

	private RefreshToken present(String rawToken) {
		return refreshTokens.findByTokenHash(oneTimeTokens.hash(rawToken)).orElseThrow(() -> new AppException(AuthError.REFRESH_TOKEN_INVALID));
	}

	private RefreshToken save(Long userId, String tokenHash, UUID familyId, ClientDetails client) {
		Instant now = clock.instant();
		return refreshTokens.saveAndFlush(RefreshToken.issued(userId, tokenHash, familyId, now,
				now.plus(properties.refreshTokenTtl()), client.userAgent(), client.ip()));
	}

	private void revokeAll(List<RefreshToken> tokens) {
		Instant now = clock.instant();
		tokens.stream().filter(token -> token.getRevokedAt() == null).forEach(token -> token.setRevokedAt(now));
	}

	private AuthTokens tokens(User user, String rawRefreshToken) {
		return new AuthTokens(jwtService.issueAccessToken(user.getId(), user.getEmail(), user.getRole()),
				properties.accessTokenTtl(), rawRefreshToken, properties.refreshTokenTtl());
	}
}
