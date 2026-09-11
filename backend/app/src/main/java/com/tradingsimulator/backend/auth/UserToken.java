package com.tradingsimulator.backend.auth;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserToken extends JpaRepository<UserTokenJpa, Long> {

	Optional<UserTokenJpa> findByTokenHashAndTokenType(String tokenHash, TokenType tokenType);
}
