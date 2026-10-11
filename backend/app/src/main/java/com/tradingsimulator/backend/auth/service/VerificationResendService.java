package com.tradingsimulator.backend.auth.service;

public interface VerificationResendService {

	void resend(String email, String password);
}
