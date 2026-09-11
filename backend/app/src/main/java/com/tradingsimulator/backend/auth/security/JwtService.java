package com.tradingsimulator.backend.auth.security;

import com.tradingsimulator.backend.auth.Role;

public interface JwtService {

	String issueAccessToken(long userId, String email, Role role);

	ParsedAccessToken verify(String token);
}
