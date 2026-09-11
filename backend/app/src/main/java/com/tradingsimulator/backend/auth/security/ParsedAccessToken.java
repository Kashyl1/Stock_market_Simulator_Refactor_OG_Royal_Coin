package com.tradingsimulator.backend.auth.security;

import com.tradingsimulator.backend.auth.Role;

public record ParsedAccessToken(long userId, String email, Role role) {
}
