package com.tradingsimulator.backend.auth.service;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.tradingsimulator.backend.auth.RefreshTokenRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RefreshTokenFamilyRevoker {

	private final RefreshTokenRepository refreshTokens;
	private final Clock clock;

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void revokeFamily(UUID familyId) {
		Instant now = clock.instant();
		refreshTokens.findByFamilyId(familyId).stream().filter(token -> token.getRevokedAt() == null).forEach(token -> token.setRevokedAt(now));
	}
}
