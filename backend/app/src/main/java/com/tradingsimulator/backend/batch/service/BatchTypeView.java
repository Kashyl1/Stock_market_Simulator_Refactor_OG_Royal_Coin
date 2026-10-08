package com.tradingsimulator.backend.batch.service;

import java.time.Instant;

import com.tradingsimulator.backend.batch.BatchJob;
import com.tradingsimulator.backend.batch.BatchJobStatus;
import com.tradingsimulator.backend.batch.BatchType;
import com.tradingsimulator.backend.batch.BatchTypeCode;

public record BatchTypeView(BatchTypeCode code, String name, String description, String cron, String zone, boolean enabled, Instant nextRunAt,
		BatchJobStatus lastStatus, Instant lastFinishedAt) {

	static BatchTypeView of(BatchType type, Instant nextRunAt, BatchJob lastFinished) {
		BatchJobStatus lastStatus = lastFinished == null ? null : lastFinished.getStatus();
		Instant lastFinishedAt = lastFinished == null ? null : lastFinished.getFinishedAt();
		return new BatchTypeView(type.getCode(), type.getName(), type.getDescription(), type.getCron(), type.getZone(), type.isEnabled(), nextRunAt, lastStatus,
				lastFinishedAt);
	}
}
