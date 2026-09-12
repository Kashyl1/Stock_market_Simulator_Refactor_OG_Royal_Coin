package com.tradingsimulator.backend.mail;

public interface MailSender {

	void sendVerificationEmail(String recipient, String verificationLink);
}
