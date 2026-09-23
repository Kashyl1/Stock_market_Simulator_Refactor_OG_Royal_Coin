package com.tradingsimulator.backend.mail;

public record VerificationEmailRequested(String recipient, String verificationLink) { }
