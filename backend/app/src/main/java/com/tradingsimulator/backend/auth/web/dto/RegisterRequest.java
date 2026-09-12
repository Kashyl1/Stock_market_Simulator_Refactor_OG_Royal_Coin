package com.tradingsimulator.backend.auth.web.dto;

import com.tradingsimulator.backend.auth.UserJpa;
import com.tradingsimulator.backend.auth.password.PasswordPolicy;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
		@NotBlank @Email @Size(max = UserJpa.EMAIL_MAX_LENGTH) String email,
		@NotBlank @Size(max = PasswordPolicy.MAX_LENGTH) String password,
		@NotBlank @Size(max = UserJpa.DISPLAY_NAME_MAX_LENGTH) String displayName) {
}
