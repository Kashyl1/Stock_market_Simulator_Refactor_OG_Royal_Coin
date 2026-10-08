package com.tradingsimulator.backend.batch;

import java.time.Instant;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "batch_job")
public class BatchJob extends BatchJobJpa {

	public static BatchJob cron(BatchType type, Instant scheduledFor) {
		return planned(type, scheduledFor, BatchJobOrigin.CRON);
	}

	public static BatchJob manual(BatchType type, Instant scheduledFor) {
		return planned(type, scheduledFor, BatchJobOrigin.MANUAL);
	}

	private static BatchJob planned(BatchType type, Instant scheduledFor, BatchJobOrigin origin) {
		BatchJob job = new BatchJob();
		job.setBatchTypeId(type.getId());
		job.setStatus(BatchJobStatus.SCHEDULED);
		job.setOrigin(origin);
		job.setScheduledFor(scheduledFor);
		return job;
	}

	public boolean isCron() {
		return getOrigin() == BatchJobOrigin.CRON;
	}

	public BatchItemCounts recordedCounts() {
		return new BatchItemCounts(getSucceededItems(), getFailedItems());
	}

	public void start(Instant now) {
		setStatus(BatchJobStatus.RUNNING);
		setStartedAt(now);
	}

	public void complete(BatchItemCounts counts, Instant now) {
		end(counts.failed() > 0 ? BatchJobStatus.COMPLETED_WITH_ERRORS : BatchJobStatus.COMPLETED, counts, now);
	}

	public void stop(BatchItemCounts counts, BatchStopRequest request, Instant now) {
		setStopRequestedBy(request.requestedBy());
		setStopRequestedAt(request.requestedAt());
		end(BatchJobStatus.STOPPED, counts, now);
	}

	public void fail(BatchItemCounts counts, BatchFailure failure, Instant now) {
		setErrorCode(failure.code());
		setErrorMessage(failure.message());
		end(BatchJobStatus.FAILED, counts, now);
	}

	public void acknowledge(String acknowledgedBy, Instant now) {
		setAcknowledgedBy(acknowledgedBy);
		setAcknowledgedAt(now);
	}

	private void end(BatchJobStatus status, BatchItemCounts counts, Instant now) {
		setStatus(status);
		setSucceededItems(counts.succeeded());
		setFailedItems(counts.failed());
		setFinishedAt(now);
	}
}
