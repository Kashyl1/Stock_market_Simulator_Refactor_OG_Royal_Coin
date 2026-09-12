package com.tradingsimulator.backend.auth;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserTokenRepository extends JpaRepository<UserToken, Long> {

	Optional<UserToken> findByTokenHashAndTokenType(String tokenHash, TokenType tokenType);
}
