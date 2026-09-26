package com.tradingsimulator.backend.auth;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import com.tradingsimulator.backend.mail.PasswordResetEmailRequested;
import com.tradingsimulator.backend.mail.VerificationEmailRequested;
import com.tradingsimulator.backend.support.TestAuthProperties;
import com.tradingsimulator.backend.support.TestUsers;

class AuthMailNotifierTest {

	private static final String RAW_TOKEN = "raw-token";
	private static final String VERIFY_LINK = TestAuthProperties.FRONTEND_BASE_URL + "/verify-email?token=" + RAW_TOKEN;
	private static final String RESET_LINK = TestAuthProperties.FRONTEND_BASE_URL + "/reset-password?token=" + RAW_TOKEN;

	private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);

	private final AuthMailNotifier notifier = new AuthMailNotifier(events, TestAuthProperties.create());

	@Test
	void asksForAVerificationMailPointingAtTheFrontend() {
		notifier.sendVerificationLink(TestUsers.EMAIL, RAW_TOKEN);

		verify(events).publishEvent(new VerificationEmailRequested(TestUsers.EMAIL, VERIFY_LINK));
	}

	@Test
	void asksForAResetMailPointingAtTheFrontend() {
		notifier.sendPasswordResetLink(TestUsers.EMAIL, RAW_TOKEN);

		verify(events).publishEvent(new PasswordResetEmailRequested(TestUsers.EMAIL, RESET_LINK));
	}
}
