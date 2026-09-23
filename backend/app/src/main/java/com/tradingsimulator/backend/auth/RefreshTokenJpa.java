package com.tradingsimulator.backend.auth;

import java.time.Instant;
import java.util.UUID;

import com.tradingsimulator.backend.common.persistence.AbstractEntity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

@MappedSuperclass
@Getter
@Setter
public class RefreshTokenJpa extends AbstractEntity {

	public static final int USER_AGENT_MAX_LENGTH = 400;
	public static final int IP_MAX_LENGTH = 64;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "token_hash", nullable = false, length = 200)
	private String tokenHash;

	@Column(name = "family_id", nullable = false)
	private UUID familyId;

	@Column(name = "issued_at", nullable = false)
	private Instant issuedAt;

	@Column(name = "expires_at", nullable = false)
	private Instant expiresAt;

	@Column(name = "revoked_at")
	private Instant revokedAt;

	@Column(name = "replaced_by")
	private Long replacedBy;

	@Column(name = "user_agent", length = USER_AGENT_MAX_LENGTH)
	private String userAgent;

	@Column(length = IP_MAX_LENGTH)
	private String ip;
}
