package com.tradingsimulator.backend.auth.service;

public interface RegistrationService {

	RegistrationResult register(String email, String password, String displayName);
}
