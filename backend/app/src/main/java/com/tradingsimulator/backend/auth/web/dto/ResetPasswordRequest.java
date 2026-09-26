package com.tradingsimulator.backend.auth.web.dto;

import com.tradingsimulator.backend.auth.password.PasswordPolicy;
import com.tradingsimulator.backend.common.Masking;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
		@NotBlank String token,
		@NotBlank @Size(max = PasswordPolicy.MAX_LENGTH) String newPassword) {

	@Override
	public String toString() {
		return "ResetPasswordRequest[token=" + Masking.MASK + ", newPassword=" + Masking.MASK + "]";
	}
}
