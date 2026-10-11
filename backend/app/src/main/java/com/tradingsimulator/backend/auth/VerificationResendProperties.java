package com.tradingsimulator.backend.auth;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Validated
@ConfigurationProperties(prefix = VerificationResendProperties.PREFIX)
public record VerificationResendProperties(@NotNull Duration cooldown, @Positive int limit, @NotNull Duration window) {

	public static final String PREFIX = "app.auth.verification-resend";
}
