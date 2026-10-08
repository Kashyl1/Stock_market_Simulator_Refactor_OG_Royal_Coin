package com.tradingsimulator.backend.batch;

import org.springframework.http.HttpStatus;

import com.tradingsimulator.backend.common.error.ErrorCode;

public enum BatchError implements ErrorCode {

	TYPE_NOT_FOUND(HttpStatus.NOT_FOUND, "batch type {0} does not exist"),
	TYPE_DISABLED(HttpStatus.CONFLICT, "batch type {0} is switched off"),
	JOB_NOT_FOUND(HttpStatus.NOT_FOUND, "batch job {0} does not exist"),
	JOB_NOT_SCHEDULED(HttpStatus.CONFLICT, "batch job {0} is {1}; only a scheduled job can be removed"),
	JOB_NOT_RUNNING(HttpStatus.CONFLICT, "batch job {0} is {1}; only a running job can be stopped"),
	JOB_NOT_ACTIVE(HttpStatus.CONFLICT, "batch job {0} is marked running but this application is not running it; it is marked failed on the next start"),
	JOB_WITHOUT_REPORT(HttpStatus.CONFLICT, "batch job {0} is {1}; there is no failure report to acknowledge"),
	HANDLER_MISSING(HttpStatus.INTERNAL_SERVER_ERROR, "no handler is registered for batch type {0}"),
	JOB_INTERRUPTED(HttpStatus.INTERNAL_SERVER_ERROR, "the application stopped while the job was running"),
	JOB_STOPPED(HttpStatus.CONFLICT, "the job was stopped by {0}");

	private static final String CODE_PREFIX = "BATCH.";

	private final HttpStatus status;
	private final String messageTemplate;

	BatchError(HttpStatus status, String messageTemplate) {
		this.status = status;
		this.messageTemplate = messageTemplate;
	}

	@Override
	public String code() {
		return CODE_PREFIX + name();
	}

	@Override
	public HttpStatus status() {
		return status;
	}

	@Override
	public String messageTemplate() {
		return messageTemplate;
	}
}
