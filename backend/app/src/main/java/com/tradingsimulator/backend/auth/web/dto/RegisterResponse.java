package com.tradingsimulator.backend.auth.web.dto;

import com.tradingsimulator.backend.auth.UserStatus;

public record RegisterResponse(long userId, UserStatus status) {
}
