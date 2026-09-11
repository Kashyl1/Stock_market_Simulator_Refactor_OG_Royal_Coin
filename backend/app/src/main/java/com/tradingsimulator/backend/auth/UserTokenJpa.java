package com.tradingsimulator.backend.auth;

import java.time.Instant;

import com.tradingsimulator.backend.common.persistence.AbstractEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "user_token")
@Getter
@Setter
public class UserTokenJpa extends AbstractEntity {

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Enumerated(EnumType.STRING)
	@Column(name = "token_type", nullable = false, length = 30)
	private TokenType tokenType;

	@Column(name = "token_hash", nullable = false, length = 200)
	private String tokenHash;

	@Column(name = "expires_at", nullable = false)
	private Instant expiresAt;

	@Column(name = "consumed_at")
	private Instant consumedAt;
}
