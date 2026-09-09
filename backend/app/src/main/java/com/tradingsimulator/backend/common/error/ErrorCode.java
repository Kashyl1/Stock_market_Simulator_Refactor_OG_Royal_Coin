package com.tradingsimulator.backend.common.error;

import org.springframework.http.HttpStatus;

public interface ErrorCode {

	String code();

	HttpStatus status();

	String messageTemplate();
}
