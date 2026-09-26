package com.tradingsimulator.backend.auth;

import java.util.Locale;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "app_user")
public class User extends UserJpa {

	public static User pending(String email, String passwordHash, String displayName) {
		User user = new User();
		user.setEmail(normalizeEmail(email));
		user.setPasswordHash(passwordHash);
		user.setDisplayName(displayName);
		user.setRole(Role.USER);
		user.setStatus(UserStatus.PENDING_VERIFICATION);
		return user;
	}

	public static User active(String email, String passwordHash, String displayName, Role role) {
		User user = pending(email, passwordHash, displayName);
		user.setRole(role);
		user.setStatus(UserStatus.ACTIVE);
		return user;
	}

	private static String normalizeEmail(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}
}
