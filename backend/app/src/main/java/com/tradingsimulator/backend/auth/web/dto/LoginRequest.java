package com.tradingsimulator.backend.auth.web.dto;

import com.tradingsimulator.backend.auth.User;
import com.tradingsimulator.backend.auth.password.PasswordPolicy;
import com.tradingsimulator.backend.common.Masking;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
		@NotBlank @Email @Size(max = User.EMAIL_MAX_LENGTH) String email,
		@NotBlank @Size(max = PasswordPolicy.MAX_LENGTH) String password) {

	@Override
	public String toString() {
		return "LoginRequest[email=" + email + ", password=" + Masking.MASK + "]";
	}
}
