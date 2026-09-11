package com.tradingsimulator.backend.auth.security;

import java.io.IOException;

import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import com.tradingsimulator.backend.common.error.CommonError;
import com.tradingsimulator.backend.common.error.ErrorResponse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

	private final ObjectMapper objectMapper;

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException authException) throws IOException {
		ErrorResponse body = ErrorResponse.of(CommonError.AUTHENTICATION_REQUIRED,
				CommonError.AUTHENTICATION_REQUIRED.messageTemplate(), request.getRequestURI());
		response.setStatus(CommonError.AUTHENTICATION_REQUIRED.status().value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		objectMapper.writeValue(response.getWriter(), body);
	}
}
