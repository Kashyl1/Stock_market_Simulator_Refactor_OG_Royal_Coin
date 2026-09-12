package com.tradingsimulator.backend.auth;

import com.tradingsimulator.backend.common.persistence.AbstractEntity;

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

@MappedSuperclass
@Getter
@Setter
public class UserJpa extends AbstractEntity {

	public static final int EMAIL_MAX_LENGTH = 320;
	public static final int DISPLAY_NAME_MAX_LENGTH = 100;

	private static final int PASSWORD_HASH_MAX_LENGTH = 200;

	@Column(nullable = false, length = EMAIL_MAX_LENGTH)
	private String email;

	@Column(name = "password_hash", nullable = false, length = PASSWORD_HASH_MAX_LENGTH)
	private String passwordHash;

	@Column(name = "display_name", nullable = false, length = DISPLAY_NAME_MAX_LENGTH)
	private String displayName;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Role role;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private UserStatus status;
}
