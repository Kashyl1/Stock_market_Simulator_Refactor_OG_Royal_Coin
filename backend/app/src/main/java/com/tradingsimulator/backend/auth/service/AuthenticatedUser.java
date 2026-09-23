package com.tradingsimulator.backend.auth.service;

import com.tradingsimulator.backend.auth.Role;
import com.tradingsimulator.backend.auth.User;
import com.tradingsimulator.backend.auth.UserStatus;

public record AuthenticatedUser(long id, String email, String displayName, Role role, UserStatus status) {

	public static AuthenticatedUser of(User user) {
		return new AuthenticatedUser(user.getId(), user.getEmail(), user.getDisplayName(), user.getRole(), user.getStatus());
	}
}
