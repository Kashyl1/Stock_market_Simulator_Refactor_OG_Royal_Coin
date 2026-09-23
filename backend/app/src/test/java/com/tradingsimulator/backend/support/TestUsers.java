package com.tradingsimulator.backend.support;

import com.tradingsimulator.backend.auth.User;
import com.tradingsimulator.backend.auth.UserStatus;

public final class TestUsers {

	public static final long USER_ID = 7L;
	public static final String EMAIL = "troodon@example.com";
	public static final String EMAIL_IN_CAPITALS = "TROODON@Example.COM";
	public static final String OTHER_EMAIL = "other.troodon@example.com";
	public static final String DISPLAY_NAME = "Troodon";
	public static final String PASSWORD = "correct-horse-battery";
	public static final String PASSWORD_HASH = "encoded-password";

	public static User active() {
		return withStatus(UserStatus.ACTIVE);
	}

	public static User withStatus(UserStatus status) {
		User user = User.pending(EMAIL, PASSWORD_HASH, DISPLAY_NAME);
		user.setStatus(status);
		return TestEntities.withId(user, USER_ID);
	}

	private TestUsers() { }
}
