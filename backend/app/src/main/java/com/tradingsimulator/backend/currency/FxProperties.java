package com.tradingsimulator.backend.currency;

import java.time.Duration;
import java.time.Period;
import java.time.ZoneId;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Validated
@ConfigurationProperties(prefix = FxProperties.PREFIX)
public record FxProperties(@NotNull ZoneId zone, @NotNull Period initialHistory, @Valid @NotNull Nbp nbp) {

	public static final String PREFIX = "app.fx";

	public record Nbp(@NotBlank String baseUrl, @Positive int daysPerRequest, @NotNull Duration connectTimeout, @NotNull Duration readTimeout) {
	}
}
