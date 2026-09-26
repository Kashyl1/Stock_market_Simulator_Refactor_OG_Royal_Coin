package com.tradingsimulator.backend.auth;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import com.tradingsimulator.backend.mail.PasswordResetEmailRequested;
import com.tradingsimulator.backend.mail.VerificationEmailRequested;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AuthMailNotifier {

	private static final String VERIFY_EMAIL_PATH = "/verify-email";
	private static final String RESET_PASSWORD_PATH = "/reset-password";
	private static final String TOKEN_QUERY_PARAM = "token";

	private final ApplicationEventPublisher events;
	private final AuthProperties properties;

	public void sendVerificationLink(String recipient, String rawToken) {
		events.publishEvent(new VerificationEmailRequested(recipient, link(VERIFY_EMAIL_PATH, rawToken)));
	}

	public void sendPasswordResetLink(String recipient, String rawToken) {
		events.publishEvent(new PasswordResetEmailRequested(recipient, link(RESET_PASSWORD_PATH, rawToken)));
	}

	private String link(String path, String rawToken) {
		return UriComponentsBuilder.fromUriString(properties.frontendBaseUrl()).path(path).queryParam(TOKEN_QUERY_PARAM, rawToken).build().toUriString();
	}
}
