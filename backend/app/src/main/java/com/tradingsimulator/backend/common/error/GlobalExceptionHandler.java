package com.tradingsimulator.backend.common.error;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

	@ExceptionHandler(AppException.class)
	public ResponseEntity<ErrorResponse> handleApp(AppException ex, HttpServletRequest request) {
		ErrorCode errorCode = ex.errorCode();
		HttpStatus status = errorCode.status();
		String where = request.getMethod() + " " + request.getRequestURI();
		if (status.is5xxServerError()) {
			log.error("{} on {}: {}", errorCode.code(), where, ex.getMessage(), ex);
		}
		else {
			log.warn("{} on {}: {}", errorCode.code(), where, ex.getMessage());
		}
		return ResponseEntity.status(status)
				.body(ErrorResponse.of(errorCode, ex.getMessage(), request.getRequestURI()));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex,
			HttpServletRequest request) {
		List<ErrorResponse.FieldError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
				.map(fe -> new ErrorResponse.FieldError(fe.getField(), fe.getDefaultMessage()))
				.toList();
		log.warn("{} on {} {}: {}", CommonError.VALIDATION_FAILED.code(), request.getMethod(),
				request.getRequestURI(), fieldErrors);
		return ResponseEntity.status(CommonError.VALIDATION_FAILED.status())
				.body(ErrorResponse.of(CommonError.VALIDATION_FAILED, CommonError.VALIDATION_FAILED.messageTemplate(),
						request.getRequestURI(), fieldErrors));
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex,
			HttpServletRequest request) {
		log.warn("{} on {} {}: {}", CommonError.CONSTRAINT_VIOLATION.code(), request.getMethod(),
				request.getRequestURI(), ex.getMostSpecificCause().getMessage());
		return ResponseEntity.status(CommonError.CONSTRAINT_VIOLATION.status())
				.body(ErrorResponse.of(CommonError.CONSTRAINT_VIOLATION,
						CommonError.CONSTRAINT_VIOLATION.messageTemplate(), request.getRequestURI()));
	}

	@ExceptionHandler(OptimisticLockingFailureException.class)
	public ResponseEntity<ErrorResponse> handleOptimisticLock(OptimisticLockingFailureException ex,
			HttpServletRequest request) {
		log.warn("{} on {} {}", CommonError.CONCURRENT_MODIFICATION.code(), request.getMethod(),
				request.getRequestURI());
		return ResponseEntity.status(CommonError.CONCURRENT_MODIFICATION.status())
				.body(ErrorResponse.of(CommonError.CONCURRENT_MODIFICATION,
						CommonError.CONCURRENT_MODIFICATION.messageTemplate(), request.getRequestURI()));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
		log.error("{} on {} {}", CommonError.INTERNAL_ERROR.code(), request.getMethod(), request.getRequestURI(), ex);
		return ResponseEntity.status(CommonError.INTERNAL_ERROR.status())
				.body(ErrorResponse.of(CommonError.INTERNAL_ERROR, CommonError.INTERNAL_ERROR.messageTemplate(),
						request.getRequestURI()));
	}
}
