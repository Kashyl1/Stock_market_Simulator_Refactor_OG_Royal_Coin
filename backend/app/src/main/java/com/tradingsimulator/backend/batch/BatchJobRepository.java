package com.tradingsimulator.backend.batch;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface BatchJobRepository extends JpaRepository<BatchJob, Long>, JpaSpecificationExecutor<BatchJob> {

	List<BatchJob> findByStatus(BatchJobStatus status);

	long countByStatus(BatchJobStatus status);

	List<BatchJob> findByStatusAndScheduledForLessThanEqualOrderByScheduledForAscIdAsc(BatchJobStatus status, Instant moment);

	boolean existsByBatchTypeId(Long batchTypeId);

	boolean existsByBatchTypeIdAndStatusAndOrigin(Long batchTypeId, BatchJobStatus status, BatchJobOrigin origin);

	Optional<BatchJob> findFirstByBatchTypeIdAndStatusOrderByScheduledForAscIdAsc(Long batchTypeId, BatchJobStatus status);

	Optional<BatchJob> findFirstByBatchTypeIdAndFinishedAtIsNotNullOrderByFinishedAtDesc(Long batchTypeId);
}
