package com.tradingsimulator.backend.batch.engine;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import com.tradingsimulator.backend.batch.BatchError;
import com.tradingsimulator.backend.batch.BatchFailure;
import com.tradingsimulator.backend.batch.BatchItemRepository;
import com.tradingsimulator.backend.batch.BatchJob;
import com.tradingsimulator.backend.batch.BatchJobRepository;
import com.tradingsimulator.backend.batch.BatchStopRequest;
import com.tradingsimulator.backend.batch.BatchTypeCode;
import com.tradingsimulator.backend.batch.BatchTypeRepository;
import com.tradingsimulator.backend.common.error.AppException;

import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

@Service
@Slf4j
public class BatchRunnerImpl implements BatchRunner {

	private final BatchJobRepository jobs;
	private final BatchTypeRepository types;
	private final BatchItemRepository items;
	private final RunningBatchJobs running;
	private final Map<BatchTypeCode, BatchJobHandler> handlers;
	private final TransactionTemplate transactions;
	private final ObjectMapper objectMapper;
	private final Clock clock;

	public BatchRunnerImpl(BatchJobRepository jobs, BatchTypeRepository types, BatchItemRepository items, RunningBatchJobs running,
			List<BatchJobHandler> handlers, PlatformTransactionManager transactionManager, ObjectMapper objectMapper, Clock clock) {
		this.jobs = jobs;
		this.types = types;
		this.items = items;
		this.running = running;
		this.handlers = handlers.stream().collect(Collectors.toUnmodifiableMap(BatchJobHandler::type, Function.identity()));
		this.transactions = new TransactionTemplate(transactionManager);
		this.transactions.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		this.objectMapper = objectMapper;
		this.clock = clock;
	}

	@Override
	public void run(Long jobId) {
		running.register(jobId);
		BatchRunImpl run = new BatchRunImpl(jobId, jobs, items, running, transactions, objectMapper, clock);
		try {
			handlerOf(jobId).run(run);
			end(jobId, job -> job.complete(run.counts(), clock.instant()));
			log.info("Batch job {} finished: {} items succeeded, {} failed", jobId, run.counts().succeeded(), run.counts().failed());
		}
		catch (RuntimeException failure) {
			end(jobId, failure, run);
		}
		finally {
			running.unregister(jobId);
		}
	}

	private void end(Long jobId, RuntimeException failure, BatchRunImpl run) {
		BatchStopRequest stopRequest = running.stopRequest(jobId).orElse(null);
		if (stopRequest != null && isStop(failure)) {
			end(jobId, job -> job.stop(run.counts(), stopRequest, clock.instant()));
			log.info("Batch job {} stopped by {}", jobId, stopRequest.requestedBy());
			return;
		}
		log.error("Batch job {} failed", jobId, failure);
		end(jobId, job -> job.fail(run.counts(), BatchFailure.of(failure), clock.instant()));
	}

	private void end(Long jobId, Consumer<BatchJob> ending) {
		transactions.executeWithoutResult(status -> ending.accept(jobOf(jobId)));
	}

	private BatchJob jobOf(Long jobId) {
		return jobs.findById(jobId).orElseThrow(() -> new AppException(BatchError.JOB_NOT_FOUND, jobId));
	}

	private BatchJobHandler handlerOf(Long jobId) {
		Long typeId = jobOf(jobId).getBatchTypeId();
		BatchTypeCode type = types.findById(typeId).orElseThrow(() -> new AppException(BatchError.TYPE_NOT_FOUND, typeId)).getCode();
		BatchJobHandler handler = handlers.get(type);
		if (handler == null) {
			throw new AppException(BatchError.HANDLER_MISSING, type);
		}
		return handler;
	}

	private static boolean isStop(RuntimeException failure) {
		return failure instanceof AppException appFailure && appFailure.errorCode() == BatchError.JOB_STOPPED;
	}
}
