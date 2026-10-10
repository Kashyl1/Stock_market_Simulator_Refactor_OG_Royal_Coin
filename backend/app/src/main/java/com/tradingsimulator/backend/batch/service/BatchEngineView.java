package com.tradingsimulator.backend.batch.service;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;

import com.tradingsimulator.backend.batch.BatchProperties;

public record BatchEngineView(boolean quiet, LocalTime quietFrom, LocalTime quietUntil, ZoneId zone, long runningJobs, boolean readyForDeploy) {

	private static final long NONE = 0;

	static BatchEngineView of(BatchProperties properties, Instant now, long runningJobs) {
		BatchProperties.QuietWindow window = properties.quietWindow();
		boolean quiet = properties.quietAt(now);
		if (window == null) {
			return new BatchEngineView(quiet, null, null, null, runningJobs, false);
		}
		return new BatchEngineView(quiet, window.start(), window.end(), window.zone(), runningJobs, quiet && runningJobs == NONE);
	}
}
