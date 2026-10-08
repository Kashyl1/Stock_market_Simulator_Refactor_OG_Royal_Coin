package com.tradingsimulator.backend.batch.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.tradingsimulator.backend.batch.BatchJob;
import com.tradingsimulator.backend.batch.BatchJobStatus;

final class BatchJobSpecifications {

	private static final String STATUS = "status";
	private static final String BATCH_TYPE_ID = "batchTypeId";
	private static final String ACKNOWLEDGED_AT = "acknowledgedAt";

	static Specification<BatchJob> matching(BatchJobFilter filter, Long batchTypeId) {
		List<Specification<BatchJob>> conditions = new ArrayList<>();
		if (!filter.statuses().isEmpty()) {
			conditions.add((job, query, criteria) -> job.get(STATUS).in(filter.statuses()));
		}
		if (batchTypeId != null) {
			conditions.add((job, query, criteria) -> criteria.equal(job.get(BATCH_TYPE_ID), batchTypeId));
		}
		if (filter.unacknowledged()) {
			conditions.add((job, query, criteria) -> criteria.and(job.get(STATUS).in(BatchJobStatus.WITH_REPORT), criteria.isNull(job.get(ACKNOWLEDGED_AT))));
		}
		return Specification.allOf(conditions);
	}

	private BatchJobSpecifications() {
	}
}
