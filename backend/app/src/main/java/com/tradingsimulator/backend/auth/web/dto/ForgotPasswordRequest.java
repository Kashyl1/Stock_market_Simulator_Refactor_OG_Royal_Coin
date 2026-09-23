package com.tradingsimulator.backend.auth.web.dto;

import com.tradingsimulator.backend.auth.UserJpa;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ForgotPasswordRequest(@NotBlank @Email @Size(max = UserJpa.EMAIL_MAX_LENGTH) String email) { }
