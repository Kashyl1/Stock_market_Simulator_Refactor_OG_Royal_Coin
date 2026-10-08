package com.tradingsimulator.backend.batch;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotNull;

@Validated
@ConfigurationProperties(prefix = BatchProperties.PREFIX)
public record BatchProperties(boolean enabled, @NotNull Duration pollInterval, @NotNull Duration shutdownTimeout) {

	public static final String PREFIX = "app.batch";
	public static final String ENABLED = PREFIX + ".enabled";
	public static final String POLL_INTERVAL = "${" + PREFIX + ".poll-interval}";
}
