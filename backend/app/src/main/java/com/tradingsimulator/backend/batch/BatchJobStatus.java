package com.tradingsimulator.backend.batch;

import java.util.Set;

public enum BatchJobStatus {
	SCHEDULED,
	RUNNING,
	COMPLETED,
	COMPLETED_WITH_ERRORS,
	FAILED,
	STOPPED;

	public static final Set<BatchJobStatus> WITH_REPORT = Set.of(COMPLETED_WITH_ERRORS, FAILED);

	public boolean hasReport() {
		return WITH_REPORT.contains(this);
	}
}
