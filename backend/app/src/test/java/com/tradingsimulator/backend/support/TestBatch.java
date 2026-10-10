package com.tradingsimulator.backend.support;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;

import com.tradingsimulator.backend.batch.BatchJob;
import com.tradingsimulator.backend.batch.BatchProperties;
import com.tradingsimulator.backend.batch.BatchType;
import com.tradingsimulator.backend.batch.BatchTypeCode;

public final class TestBatch {

	public static final long TYPE_ID = 3L;
	public static final String HOURLY = "0 0 * * * *";
	public static final String UTC = "UTC";
	public static final String TYPE_NAME = "FX rates from NBP";
	public static final String TYPE_DESCRIPTION = "Stores the NBP mid rates";
	public static final Instant AT_10_00 = Instant.parse("2026-10-08T10:00:00Z");
	public static final Instant AT_10_02 = Instant.parse("2026-10-08T10:02:00Z");
	public static final Instant AT_11_00 = Instant.parse("2026-10-08T11:00:00Z");
	public static final Instant AT_12_00 = Instant.parse("2026-10-08T12:00:00Z");
	public static final Instant AT_13_00 = Instant.parse("2026-10-08T13:00:00Z");
	public static final LocalTime QUIET_FROM = LocalTime.MIDNIGHT;
	public static final LocalTime QUIET_UNTIL = LocalTime.of(2, 30);

	private static final Duration POLL_INTERVAL = Duration.ofSeconds(15);
	private static final Duration SHUTDOWN_TIMEOUT = Duration.ofSeconds(30);

	public static BatchProperties properties() {
		return new BatchProperties(true, POLL_INTERVAL, SHUTDOWN_TIMEOUT, new BatchProperties.QuietWindow(QUIET_FROM, QUIET_UNTIL, ZoneId.of(UTC)));
	}

	public static BatchType hourlyType() {
		return TestEntities.withId(BatchType.scheduled(BatchTypeCode.FX_RATES_NBP, TYPE_NAME, TYPE_DESCRIPTION, HOURLY, UTC), TYPE_ID);
	}

	public static BatchJob cronJob(long id, Instant scheduledFor) {
		return TestEntities.withId(BatchJob.cron(hourlyType(), scheduledFor), id);
	}

	public static BatchJob manualJob(long id, Instant scheduledFor) {
		return TestEntities.withId(BatchJob.manual(hourlyType(), scheduledFor), id);
	}

	public static BatchJob runningJob(long id, Instant startedAt) {
		BatchJob job = manualJob(id, startedAt);
		job.start(startedAt);
		return job;
	}

	private TestBatch() {
	}
}
