package com.tradingsimulator.backend.batch;

import java.util.Objects;

import com.tradingsimulator.backend.common.error.AppException;
import com.tradingsimulator.backend.common.error.CommonError;

public record BatchFailure(String code, String message) {

	public static final int CODE_MAX_LENGTH = 100;
	public static final int MESSAGE_MAX_LENGTH = 500;

	private static final String TYPE_SEPARATOR = ": ";

	public static BatchFailure of(Throwable failure) {
		if (failure instanceof AppException appFailure) {
			return new BatchFailure(appFailure.errorCode().code(), shorten(appFailure.getMessage()));
		}
		String detail = Objects.requireNonNullElse(failure.getMessage(), failure.getClass().getName());
		return new BatchFailure(CommonError.INTERNAL_ERROR.code(), shorten(failure.getClass().getSimpleName() + TYPE_SEPARATOR + detail));
	}

	private static String shorten(String message) {
		return message.length() <= MESSAGE_MAX_LENGTH ? message : message.substring(0, MESSAGE_MAX_LENGTH);
	}
}
