package com.tradingsimulator.backend.auth.password;

public interface PasswordPolicy {

	int MIN_LENGTH = 10;
	int MAX_LENGTH = 72;

	void check(String password);
}
