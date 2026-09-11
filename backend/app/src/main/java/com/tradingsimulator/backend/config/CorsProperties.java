package com.tradingsimulator.backend.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotEmpty;

@Validated
@ConfigurationProperties(prefix = CorsProperties.PREFIX)
public record CorsProperties(@NotEmpty List<String> allowedOrigins) {

	public static final String PREFIX = "app.cors";
}
