package com.tradingsimulator.backend.batch.service;

import java.util.Set;

import com.tradingsimulator.backend.batch.BatchJobStatus;
import com.tradingsimulator.backend.batch.BatchTypeCode;

public record BatchJobFilter(Set<BatchJobStatus> statuses, BatchTypeCode type, boolean unacknowledged) {

	public BatchJobFilter {
		statuses = statuses == null ? Set.of() : Set.copyOf(statuses);
	}
}
