package com.tradingsimulator.backend.mail;

public record PasswordResetEmailRequested(String recipient, String resetLink) { }
