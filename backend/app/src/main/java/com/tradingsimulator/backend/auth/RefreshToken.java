package com.tradingsimulator.backend.auth;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshToken extends JpaRepository<RefreshTokenJpa, Long> {

	Optional<RefreshTokenJpa> findByTokenHash(String tokenHash);

	List<RefreshTokenJpa> findByFamilyId(UUID familyId);
}
