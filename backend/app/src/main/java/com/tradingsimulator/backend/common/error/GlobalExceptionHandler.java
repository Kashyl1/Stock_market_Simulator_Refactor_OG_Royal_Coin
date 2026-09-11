package com.tradingsimulator.backend.common.error;

import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	private static final Map<Integer, CommonError> FRAMEWORK_ERRORS_BY_STATUS = Map.of(
			HttpStatus.NOT_FOUND.value(), CommonError.ENDPOINT_NOT_FOUND,
			HttpStatus.METHOD_NOT_ALLOWED.value(), CommonError.METHOD_NOT_ALLOWED,
			HttpStatus.NOT_ACCEPTABLE.value(), CommonError.NOT_ACCEPTABLE,
			HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(), CommonError.UNSUPPORTED_MEDIA_TYPE);

	@ExceptionHandler(AppException.class)
	public ResponseEntity<ErrorResponse> handleApp(AppException ex, HttpServletRequest request) {
		ErrorCode errorCode = ex.errorCode();
		if (errorCode.status().is5xxServerError()) {
			log.error("{} on {}: {}", errorCode.code(), where(request), ex.getMessage(), ex);
		}
		else {
			log.warn("{} on {}: {}", errorCode.code(), where(request), ex.getMessage());
		}
		return ResponseEntity.status(errorCode.status())
				.body(ErrorResponse.of(errorCode, ex.getMessage(), request.getRequestURI()));
	}

	@ExceptionHandler({ AccessDeniedException.class, AuthenticationException.class })
	public void rethrowToSecurityFilters(RuntimeException ex) {
		throw ex;
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex,
			HttpServletRequest request) {
		log.warn("{} on {}: {}", CommonError.CONSTRAINT_VIOLATION.code(), where(request),
				ex.getMostSpecificCause().getMessage());
		return respond(CommonError.CONSTRAINT_VIOLATION, request);
	}

	@ExceptionHandler(OptimisticLockingFailureException.class)
	public ResponseEntity<ErrorResponse> handleOptimisticLock(OptimisticLockingFailureException ex,
			HttpServletRequest request) {
		log.warn("{} on {}", CommonError.CONCURRENT_MODIFICATION.code(), where(request));
		return respond(CommonError.CONCURRENT_MODIFICATION, request);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
		log.error("{} on {}", CommonError.INTERNAL_ERROR.code(), where(request), ex);
		return respond(CommonError.INTERNAL_ERROR, request);
	}

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest webRequest) {
		HttpServletRequest request = servletRequest(webRequest);
		List<ErrorResponse.FieldError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
				.map(fe -> new ErrorResponse.FieldError(fe.getField(), fe.getDefaultMessage()))
				.toList();
		log.warn("{} on {}: {}", CommonError.VALIDATION_FAILED.code(), where(request), fieldErrors);
		return ResponseEntity.status(CommonError.VALIDATION_FAILED.status())
				.headers(headers)
				.<Object>body(ErrorResponse.of(CommonError.VALIDATION_FAILED,
						CommonError.VALIDATION_FAILED.messageTemplate(), request.getRequestURI(), fieldErrors));
	}

	@Override
	protected ResponseEntity<Object> handleExceptionInternal(Exception ex, @Nullable Object body,
			HttpHeaders headers, HttpStatusCode statusCode, WebRequest webRequest) {
		HttpServletRequest request = servletRequest(webRequest);
		CommonError error = frameworkErrorFor(statusCode);
		if (statusCode.is5xxServerError()) {
			log.error("{} on {}", error.code(), where(request), ex);
		}
		else {
			log.warn("{} on {}: {}", error.code(), where(request), ex.getMessage());
		}
		return ResponseEntity.status(error.status())
				.headers(headers)
				.<Object>body(ErrorResponse.of(error, error.messageTemplate(), request.getRequestURI()));
	}

	private static CommonError frameworkErrorFor(HttpStatusCode statusCode) {
		CommonError fallback = statusCode.is4xxClientError() ? CommonError.MALFORMED_REQUEST : CommonError.INTERNAL_ERROR;
		return FRAMEWORK_ERRORS_BY_STATUS.getOrDefault(statusCode.value(), fallback);
	}

	private static ResponseEntity<ErrorResponse> respond(CommonError error, HttpServletRequest request) {
		return ResponseEntity.status(error.status())
				.body(ErrorResponse.of(error, error.messageTemplate(), request.getRequestURI()));
	}

	private static HttpServletRequest servletRequest(WebRequest webRequest) {
		return ((ServletWebRequest) webRequest).getRequest();
	}

	private static String where(HttpServletRequest request) {
		return request.getMethod() + " " + request.getRequestURI();
	}
}
