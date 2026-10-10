package com.tradingsimulator.backend.batch;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

@Validated
@ConfigurationProperties(prefix = BatchProperties.PREFIX)
public record BatchProperties(boolean enabled, @NotNull Duration pollInterval, @NotNull Duration shutdownTimeout, @Valid QuietWindow quietWindow) {

	public static final String PREFIX = "app.batch";
	public static final String ENABLED = PREFIX + ".enabled";
	public static final String POLL_INTERVAL = "${" + PREFIX + ".poll-interval}";

	public boolean quietAt(Instant moment) {
		return quietWindow != null && quietWindow.contains(moment);
	}

	public record QuietWindow(@NotNull @DateTimeFormat(pattern = QuietWindow.TIME_FORMAT) LocalTime start,
			@NotNull @DateTimeFormat(pattern = QuietWindow.TIME_FORMAT) LocalTime end, @NotNull ZoneId zone) {

		public static final String TIME_FORMAT = "HH:mm";

		public boolean contains(Instant moment) {
			if (start.equals(end)) {
				return false;
			}
			LocalTime time = LocalTime.ofInstant(moment, zone);
			boolean fromStart = !time.isBefore(start);
			boolean beforeEnd = time.isBefore(end);
			return start.isBefore(end) ? fromStart && beforeEnd : fromStart || beforeEnd;
		}
	}
}
