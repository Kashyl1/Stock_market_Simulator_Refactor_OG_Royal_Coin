package com.tradingsimulator.backend.auth.service;

public interface EmailVerificationService {

	void verify(String rawToken);
}
