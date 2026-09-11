package com.tradingsimulator.backend.common.error;

import org.springframework.http.HttpStatus;

public enum CommonError implements ErrorCode {

	RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "{0} with id {1} was not found"),
	VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "the request is not valid"),
	MALFORMED_REQUEST(HttpStatus.BAD_REQUEST, "the request could not be read"),
	ENDPOINT_NOT_FOUND(HttpStatus.NOT_FOUND, "no endpoint matches this request"),
	METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "this HTTP method is not supported by the endpoint"),
	NOT_ACCEPTABLE(HttpStatus.NOT_ACCEPTABLE, "the requested response format is not available"),
	UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "this content type is not supported"),
	CONSTRAINT_VIOLATION(HttpStatus.CONFLICT, "the request conflicts with existing data"),
	CONCURRENT_MODIFICATION(HttpStatus.CONFLICT, "the resource was changed by someone else; reload and retry"),
	AUTHENTICATION_REQUIRED(HttpStatus.UNAUTHORIZED, "authentication is required"),
	ACCESS_DENIED(HttpStatus.FORBIDDEN, "you do not have access to this resource"),
	INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "an unexpected error occurred");

	private static final String CODE_PREFIX = "COMMON.";

	private final HttpStatus status;
	private final String messageTemplate;

	CommonError(HttpStatus status, String messageTemplate) {
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
