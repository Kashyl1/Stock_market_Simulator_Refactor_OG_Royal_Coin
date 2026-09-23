package com.tradingsimulator.backend.auth.service;

public interface PasswordResetService {

	void requestReset(String email);

	void reset(String rawToken, String newPassword);
}
