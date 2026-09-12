package com.tradingsimulator.backend.auth.token;

public interface OneTimeTokenService {

	OneTimeToken issue();

	String hash(String rawToken);
}
