package com.tradingsimulator.backend.auth.web.dto;

import com.tradingsimulator.backend.auth.Role;
import com.tradingsimulator.backend.auth.UserStatus;
import com.tradingsimulator.backend.auth.service.AuthenticatedUser;

public record CurrentUserResponse(long id, String email, String displayName, Role role, UserStatus status) {

	public static CurrentUserResponse of(AuthenticatedUser user) {
		return new CurrentUserResponse(user.id(), user.email(), user.displayName(), user.role(), user.status());
	}
}
