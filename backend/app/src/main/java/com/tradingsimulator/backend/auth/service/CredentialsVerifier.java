package com.tradingsimulator.backend.auth.service;

import com.tradingsimulator.backend.auth.User;

public interface CredentialsVerifier {

	User verify(String email, String password);
}
