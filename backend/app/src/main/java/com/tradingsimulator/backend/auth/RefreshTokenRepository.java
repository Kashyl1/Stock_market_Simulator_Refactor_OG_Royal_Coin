package com.tradingsimulator.backend.auth;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

	Optional<RefreshToken> findByTokenHash(String tokenHash);

	List<RefreshToken> findByFamilyId(UUID familyId);

	List<RefreshToken> findByUserIdAndRevokedAtIsNull(Long userId);
}
