package com.tradingsimulator.backend.batch.engine;

import java.time.Clock;

import org.springframework.transaction.support.TransactionOperations;

import com.tradingsimulator.backend.batch.BatchError;
import com.tradingsimulator.backend.batch.BatchFailure;
import com.tradingsimulator.backend.batch.BatchItem;
import com.tradingsimulator.backend.batch.BatchItemCounts;
import com.tradingsimulator.backend.batch.BatchItemRepository;
import com.tradingsimulator.backend.batch.BatchJob;
import com.tradingsimulator.backend.batch.BatchJobRepository;
import com.tradingsimulator.backend.common.error.AppException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@RequiredArgsConstructor
class BatchRunImpl implements BatchRun {

	private final Long jobId;
	private final BatchJobRepository jobs;
	private final BatchItemRepository items;
	private final RunningBatchJobs running;
	private final TransactionOperations transactions;
	private final ObjectMapper objectMapper;
	private final Clock clock;

	private int succeeded;
	private int failed;

	@Override
	public void totalItems(int total) {
		transactions.executeWithoutResult(status -> job().setTotalItems(total));
	}

	@Override
	public void item(String key, BatchItemWork work) {
		running.stopRequest(jobId).ifPresent(request -> {
			throw new AppException(BatchError.JOB_STOPPED, request.requestedBy());
		});
		try {
			transactions.executeWithoutResult(status -> items.save(BatchItem.succeeded(jobId, key, payloadOf(work.process()), clock.instant())));
			succeeded++;
		}
		catch (RuntimeException failure) {
			log.warn("Batch job {} item {} failed", jobId, key, failure);
			transactions.executeWithoutResult(status -> items.save(BatchItem.failed(jobId, key, BatchFailure.of(failure), clock.instant())));
			failed++;
		}
	}

	BatchItemCounts counts() {
		return new BatchItemCounts(succeeded, failed);
	}

	private BatchJob job() {
		return jobs.findById(jobId).orElseThrow(() -> new AppException(BatchError.JOB_NOT_FOUND, jobId));
	}

	private String payloadOf(Object result) {
		return result == null ? null : objectMapper.writeValueAsString(result);
	}
}
