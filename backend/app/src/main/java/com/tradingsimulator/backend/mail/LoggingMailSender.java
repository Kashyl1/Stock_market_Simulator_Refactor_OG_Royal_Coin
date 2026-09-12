package com.tradingsimulator.backend.mail;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class LoggingMailSender implements MailSender {

	@Override
	public void sendVerificationEmail(String recipient, String verificationLink) {
		log.info("Verification e-mail for {}: {}", recipient, verificationLink);
	}
}
