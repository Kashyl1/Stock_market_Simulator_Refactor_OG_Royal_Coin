package com.tradingsimulator.backend.auth;

import com.tradingsimulator.backend.common.persistence.AbstractEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "app_user")
@Getter
@Setter
public class UserJpa extends AbstractEntity {

	@Column(nullable = false, length = 320)
	private String email;

	@Column(name = "password_hash", nullable = false, length = 200)
	private String passwordHash;

	@Column(name = "display_name", nullable = false, length = 100)
	private String displayName;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Role role;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private UserStatus status;
}
