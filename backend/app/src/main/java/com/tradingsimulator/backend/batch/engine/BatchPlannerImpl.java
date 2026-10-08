package com.tradingsimulator.backend.batch.engine;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tradingsimulator.backend.batch.BatchError;
import com.tradingsimulator.backend.batch.BatchFailure;
import com.tradingsimulator.backend.batch.BatchItemRepository;
import com.tradingsimulator.backend.batch.BatchJob;
import com.tradingsimulator.backend.batch.BatchJobOrigin;
import com.tradingsimulator.backend.batch.BatchJobRepository;
import com.tradingsimulator.backend.batch.BatchJobStatus;
import com.tradingsimulator.backend.batch.BatchType;
import com.tradingsimulator.backend.batch.BatchTypeRepository;
import com.tradingsimulator.backend.common.error.AppException;
import com.tradingsimulator.backend.common.persistence.HistoryActor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class BatchPlannerImpl implements BatchPlanner {

	private final BatchTypeRepository types;
	private final BatchJobRepository jobs;
	private final BatchItemRepository items;
	private final HistoryActor historyActor;
	private final Clock clock;

	@Override
	@Transactional
	public void recover() {
		Instant now = clock.instant();
		BatchFailure interrupted = BatchFailure.of(new AppException(BatchError.JOB_INTERRUPTED));
		for (BatchJob job : jobs.findByStatus(BatchJobStatus.RUNNING)) {
			job.fail(items.countsOf(job.getId()), interrupted, now);
			log.warn("Batch job {} was still running when the application stopped; marked as failed", job.getId());
		}
		types.findAll().stream().filter(BatchType::runsOnSchedule).filter(this::hasNoPlannedCronJob).forEach(type -> planFirstCronJob(type, now));
	}

	@Override
	@Transactional
	public List<Long> startDueJobs() {
		Instant now = clock.instant();
		Map<Long, BatchType> typesById = types.findAll().stream().collect(Collectors.toMap(BatchType::getId, Function.identity()));
		Set<Long> busyTypes = jobs.findByStatus(BatchJobStatus.RUNNING).stream().map(BatchJob::getBatchTypeId).collect(Collectors.toCollection(HashSet::new));
		List<Long> started = new ArrayList<>();
		for (BatchJob due : jobs.findByStatusAndScheduledForLessThanEqualOrderByScheduledForAscIdAsc(BatchJobStatus.SCHEDULED, now)) {
			BatchType type = typesById.get(due.getBatchTypeId());
			if (!busyTypes.contains(type.getId())) {
				start(due, type, now);
				busyTypes.add(type.getId());
				started.add(due.getId());
			}
			else if (due.isCron()) {
				dropOverrun(due, type, now);
			}
		}
		return List.copyOf(started);
	}

	@Override
	@Transactional
	public void remove(Long jobId) {
		BatchJob job = jobs.findById(jobId).orElseThrow(() -> new AppException(BatchError.JOB_NOT_FOUND, jobId));
		if (job.getStatus() != BatchJobStatus.SCHEDULED) {
			throw new AppException(BatchError.JOB_NOT_SCHEDULED, jobId, job.getStatus());
		}
		delete(job);
		if (job.isCron()) {
			Instant now = clock.instant();
			planNextCronJob(typeOf(job), job.getScheduledFor().isAfter(now) ? job.getScheduledFor() : now);
		}
		log.info("Batch job {} planned for {} was removed", jobId, job.getScheduledFor());
	}

	private void start(BatchJob job, BatchType type, Instant now) {
		job.start(now);
		jobs.flush();
		if (job.isCron()) {
			planNextCronJob(type, now);
		}
		log.info("Batch job {} of {} started", job.getId(), type.getCode());
	}

	private void dropOverrun(BatchJob job, BatchType type, Instant now) {
		delete(job);
		planNextCronJob(type, now);
		log.info("Batch job {} of {} planned for {} was dropped: a job of its type was still running", job.getId(), type.getCode(), job.getScheduledFor());
	}

	private void delete(BatchJob job) {
		historyActor.declareCurrentAuditor();
		jobs.delete(job);
		jobs.flush();
	}

	private boolean hasNoPlannedCronJob(BatchType type) {
		return !jobs.existsByBatchTypeIdAndStatusAndOrigin(type.getId(), BatchJobStatus.SCHEDULED, BatchJobOrigin.CRON);
	}

	private void planFirstCronJob(BatchType type, Instant now) {
		if (jobs.existsByBatchTypeId(type.getId())) {
			planNextCronJob(type, now);
			return;
		}
		jobs.save(BatchJob.cron(type, now));
		log.info("Batch type {} has never run; its first job starts now", type.getCode());
	}

	private void planNextCronJob(BatchType type, Instant after) {
		type.nextRunAfter(after).ifPresent(next -> jobs.save(BatchJob.cron(type, next)));
	}

	private BatchType typeOf(BatchJob job) {
		return types.findById(job.getBatchTypeId()).orElseThrow(() -> new AppException(BatchError.TYPE_NOT_FOUND, job.getBatchTypeId()));
	}
}
