package com.tradingsimulator.backend.batch.service;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;

import com.tradingsimulator.backend.batch.BatchEngine;
import com.tradingsimulator.backend.batch.BatchProperties;

public record BatchEngineView(boolean on, String switchedBy, Instant switchedAt, boolean quiet, LocalTime quietFrom, LocalTime quietUntil, ZoneId zone,
		long runningJobs, boolean readyForDeploy) {

	private static final long NONE = 0;

	static BatchEngineView of(BatchEngine engine, BatchProperties properties, Instant now, long runningJobs) {
		BatchProperties.QuietWindow window = properties.quietWindow();
		boolean on = !engine.isPaused();
		boolean quiet = properties.quietAt(now);
		boolean readyForDeploy = (quiet || !on) && runningJobs == NONE;
		LocalTime quietFrom = window == null ? null : window.start();
		LocalTime quietUntil = window == null ? null : window.end();
		ZoneId zone = window == null ? null : window.zone();
		return new BatchEngineView(on, engine.getChangedBy(), engine.getChangedWhen(), quiet, quietFrom, quietUntil, zone, runningJobs, readyForDeploy);
	}
}
