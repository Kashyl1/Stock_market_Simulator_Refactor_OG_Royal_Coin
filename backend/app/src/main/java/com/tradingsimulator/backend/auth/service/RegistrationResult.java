package com.tradingsimulator.backend.auth.service;

import com.tradingsimulator.backend.auth.UserStatus;

public record RegistrationResult(long userId, UserStatus status) {
}
