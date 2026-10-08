package com.tradingsimulator.backend.batch.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.AuditorAware;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tradingsimulator.backend.batch.BatchError;
import com.tradingsimulator.backend.batch.BatchItem;
import com.tradingsimulator.backend.batch.BatchItemCounts;
import com.tradingsimulator.backend.batch.BatchItemRepository;
import com.tradingsimulator.backend.batch.BatchItemStatus;
import com.tradingsimulator.backend.batch.BatchJob;
import com.tradingsimulator.backend.batch.BatchJobRepository;
import com.tradingsimulator.backend.batch.BatchJobStatus;
import com.tradingsimulator.backend.batch.BatchStopRequest;
import com.tradingsimulator.backend.batch.BatchType;
import com.tradingsimulator.backend.batch.BatchTypeCode;
import com.tradingsimulator.backend.batch.BatchTypeRepository;
import com.tradingsimulator.backend.batch.engine.BatchPlanner;
import com.tradingsimulator.backend.batch.engine.RunningBatchJobs;
import com.tradingsimulator.backend.common.error.AppException;
import com.tradingsimulator.backend.common.persistence.JpaAuditingConfig;
import com.tradingsimulator.backend.web.PageResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BatchAdminServiceImpl implements BatchAdminService {

	private static final String CODE = "code";
	private static final String SCHEDULED_FOR = "scheduledFor";
	private static final String ID = "id";
	private static final Sort NEWEST_FIRST = Sort.by(Sort.Direction.DESC, SCHEDULED_FOR, ID);
	private static final Sort IN_PROCESSING_ORDER = Sort.by(ID);

	private final BatchTypeRepository types;
	private final BatchJobRepository jobs;
	private final BatchItemRepository items;
	private final BatchPlanner planner;
	private final RunningBatchJobs running;
	private final AuditorAware<String> auditorAware;
	private final Clock clock;

	@Override
	@Transactional(readOnly = true)
	public List<BatchTypeView> types() {
		return types.findAll(Sort.by(CODE)).stream().map(this::viewOf).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public PageResponse<BatchJobView> jobs(BatchJobFilter filter, int page, int size) {
		Long batchTypeId = filter.type() == null ? null : typeOf(filter.type()).getId();
		Map<Long, BatchTypeCode> typeCodes = typeCodes();
		Page<BatchJob> found = jobs.findAll(BatchJobSpecifications.matching(filter, batchTypeId), PageRequest.of(page, size, NEWEST_FIRST));
		return PageResponse.of(found, job -> viewOf(job, typeCodes));
	}

	@Override
	@Transactional(readOnly = true)
	public BatchJobView job(Long jobId) {
		return viewOf(jobOf(jobId), typeCodes());
	}

	@Override
	@Transactional(readOnly = true)
	public PageResponse<BatchItemView> items(Long jobId, BatchItemStatus status, int page, int size) {
		Long existingJobId = jobOf(jobId).getId();
		return PageResponse.of(itemsOf(existingJobId, status, PageRequest.of(page, size, IN_PROCESSING_ORDER)), BatchItemView::of);
	}

	@Override
	@Transactional
	public BatchJobView plan(BatchTypeCode code, Instant scheduledFor) {
		BatchType type = typeOf(code);
		if (!type.isEnabled()) {
			throw new AppException(BatchError.TYPE_DISABLED, code);
		}
		Instant now = clock.instant();
		Instant start = scheduledFor == null || scheduledFor.isBefore(now) ? now : scheduledFor;
		return viewOf(jobs.save(BatchJob.manual(type, start)), Map.of(type.getId(), code));
	}

	@Override
	public void remove(Long jobId) {
		planner.remove(jobId);
	}

	@Override
	@Transactional(readOnly = true)
	public void stop(Long jobId) {
		BatchJob job = jobOf(jobId);
		if (job.getStatus() != BatchJobStatus.RUNNING) {
			throw new AppException(BatchError.JOB_NOT_RUNNING, jobId, job.getStatus());
		}
		if (!running.requestStop(jobId, new BatchStopRequest(currentAuditor(), clock.instant()))) {
			throw new AppException(BatchError.JOB_NOT_ACTIVE, jobId);
		}
	}

	@Override
	@Transactional
	public BatchJobView acknowledge(Long jobId) {
		BatchJob job = jobOf(jobId);
		if (!job.getStatus().hasReport()) {
			throw new AppException(BatchError.JOB_WITHOUT_REPORT, jobId, job.getStatus());
		}
		if (job.getAcknowledgedAt() == null) {
			job.acknowledge(currentAuditor(), clock.instant());
		}
		return viewOf(job, typeCodes());
	}

	private BatchTypeView viewOf(BatchType type) {
		Optional<BatchJob> next = jobs.findFirstByBatchTypeIdAndStatusOrderByScheduledForAscIdAsc(type.getId(), BatchJobStatus.SCHEDULED);
		Instant nextRunAt = next.map(BatchJob::getScheduledFor).orElse(null);
		BatchJob lastFinished = jobs.findFirstByBatchTypeIdAndFinishedAtIsNotNullOrderByFinishedAtDesc(type.getId()).orElse(null);
		return BatchTypeView.of(type, nextRunAt, lastFinished);
	}

	private BatchJobView viewOf(BatchJob job, Map<Long, BatchTypeCode> typeCodes) {
		BatchItemCounts counts = job.getStatus() == BatchJobStatus.RUNNING ? items.countsOf(job.getId()) : job.recordedCounts();
		return BatchJobView.of(job, typeCodes.get(job.getBatchTypeId()), counts);
	}

	private Page<BatchItem> itemsOf(Long jobId, BatchItemStatus status, PageRequest pageRequest) {
		return status == null ? items.findByBatchJobId(jobId, pageRequest) : items.findByBatchJobIdAndStatus(jobId, status, pageRequest);
	}

	private Map<Long, BatchTypeCode> typeCodes() {
		return types.findAll().stream().collect(Collectors.toUnmodifiableMap(BatchType::getId, BatchType::getCode));
	}

	private BatchType typeOf(BatchTypeCode code) {
		return types.findByCode(code).orElseThrow(() -> new AppException(BatchError.TYPE_NOT_FOUND, code));
	}

	private BatchJob jobOf(Long jobId) {
		return jobs.findById(jobId).orElseThrow(() -> new AppException(BatchError.JOB_NOT_FOUND, jobId));
	}

	private String currentAuditor() {
		return auditorAware.getCurrentAuditor().orElse(JpaAuditingConfig.SYSTEM_AUDITOR);
	}
}
