package com.tradingsimulator.backend.batch.service;

import java.time.Instant;

import com.tradingsimulator.backend.batch.BatchItemCounts;
import com.tradingsimulator.backend.batch.BatchJob;
import com.tradingsimulator.backend.batch.BatchJobOrigin;
import com.tradingsimulator.backend.batch.BatchJobStatus;
import com.tradingsimulator.backend.batch.BatchTypeCode;

public record BatchJobView(Long id, BatchTypeCode type, BatchJobStatus status, BatchJobOrigin origin, Instant scheduledFor, String plannedBy,
		Instant startedAt, Instant finishedAt, Integer totalItems, int succeededItems, int failedItems, String errorCode, String errorMessage,
		String stopRequestedBy, Instant stopRequestedAt, String acknowledgedBy, Instant acknowledgedAt) {

	static BatchJobView of(BatchJob job, BatchTypeCode type, BatchItemCounts counts) {
		return new BatchJobView(job.getId(), type, job.getStatus(), job.getOrigin(), job.getScheduledFor(), job.getCreatedBy(), job.getStartedAt(),
				job.getFinishedAt(), job.getTotalItems(), counts.succeeded(), counts.failed(), job.getErrorCode(), job.getErrorMessage(),
				job.getStopRequestedBy(), job.getStopRequestedAt(), job.getAcknowledgedBy(), job.getAcknowledgedAt());
	}
}
