package com.tradingsimulator.backend.support;

import java.time.Instant;

import com.tradingsimulator.backend.batch.BatchJob;
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
