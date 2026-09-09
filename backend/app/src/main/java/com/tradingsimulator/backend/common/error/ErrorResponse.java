package com.tradingsimulator.backend.common.error;

import java.time.Instant;
import java.util.List;

public record ErrorResponse(
		String code,
		String message,
		int status,
		Instant timestamp,
		String path,
		List<FieldError> fieldErrors) {

	public record FieldError(String field, String message) {
	}

	public static ErrorResponse of(ErrorCode errorCode, String message, String path) {
		return new ErrorResponse(errorCode.code(), message, errorCode.status().value(), Instant.now(), path, List.of());
	}

	public static ErrorResponse of(ErrorCode errorCode, String message, String path, List<FieldError> fieldErrors) {
		return new ErrorResponse(errorCode.code(), message, errorCode.status().value(), Instant.now(), path, fieldErrors);
	}
}
