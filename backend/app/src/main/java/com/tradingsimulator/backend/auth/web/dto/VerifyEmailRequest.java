package com.tradingsimulator.backend.auth.web.dto;

import com.tradingsimulator.backend.common.Masking;

import jakarta.validation.constraints.NotBlank;

public record VerifyEmailRequest(@NotBlank String token) {

	@Override
	public String toString() {
		return "VerifyEmailRequest[token=" + Masking.MASK + "]";
	}
}
