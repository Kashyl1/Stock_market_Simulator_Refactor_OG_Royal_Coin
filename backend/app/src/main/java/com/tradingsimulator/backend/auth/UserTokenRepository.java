package com.tradingsimulator.backend.auth;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserTokenRepository extends JpaRepository<UserToken, Long> {

	Optional<UserToken> findByTokenHashAndTokenType(String tokenHash, TokenType tokenType);

	boolean existsByUserIdAndTokenTypeAndCreatedWhenAfter(Long userId, TokenType tokenType, Instant after);

	long countByUserIdAndTokenTypeAndCreatedWhenAfter(Long userId, TokenType tokenType, Instant after);

	List<UserToken> findByUserIdAndTokenTypeAndConsumedAtIsNull(Long userId, TokenType tokenType);
}
