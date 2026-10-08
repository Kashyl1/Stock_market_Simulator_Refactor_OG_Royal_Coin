package com.tradingsimulator.backend.batch;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BatchItemRepository extends JpaRepository<BatchItem, Long> {

	default BatchItemCounts countsOf(Long batchJobId) {
		int succeeded = Math.toIntExact(countByBatchJobIdAndStatus(batchJobId, BatchItemStatus.SUCCEEDED));
		int failed = Math.toIntExact(countByBatchJobIdAndStatus(batchJobId, BatchItemStatus.FAILED));
		return new BatchItemCounts(succeeded, failed);
	}

	long countByBatchJobIdAndStatus(Long batchJobId, BatchItemStatus status);

	Page<BatchItem> findByBatchJobId(Long batchJobId, Pageable pageable);

	Page<BatchItem> findByBatchJobIdAndStatus(Long batchJobId, BatchItemStatus status, Pageable pageable);
}
