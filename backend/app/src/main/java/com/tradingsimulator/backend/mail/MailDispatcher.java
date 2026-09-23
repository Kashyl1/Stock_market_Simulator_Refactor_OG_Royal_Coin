package com.tradingsimulator.backend.mail;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MailDispatcher {

	private final MailSender mailSender;

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onVerificationEmailRequested(VerificationEmailRequested request) {
		mailSender.sendVerificationEmail(request.recipient(), request.verificationLink());
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onPasswordResetEmailRequested(PasswordResetEmailRequested request) {
		mailSender.sendPasswordResetEmail(request.recipient(), request.resetLink());
	}
}
