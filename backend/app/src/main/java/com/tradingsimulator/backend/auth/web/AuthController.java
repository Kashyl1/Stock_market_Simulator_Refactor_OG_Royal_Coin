package com.tradingsimulator.backend.auth.web;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.tradingsimulator.backend.auth.AuthPaths;
import com.tradingsimulator.backend.auth.service.EmailVerificationService;
import com.tradingsimulator.backend.auth.service.RegistrationResult;
import com.tradingsimulator.backend.auth.service.RegistrationService;
import com.tradingsimulator.backend.auth.web.dto.RegisterRequest;
import com.tradingsimulator.backend.auth.web.dto.RegisterResponse;
import com.tradingsimulator.backend.auth.web.dto.VerifyEmailRequest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(AuthPaths.BASE)
@RequiredArgsConstructor
public class AuthController {

	private final RegistrationService registrationService;
	private final EmailVerificationService emailVerificationService;

	@PostMapping(AuthPaths.REGISTER)
	@ResponseStatus(HttpStatus.CREATED)
	public RegisterResponse register(@Valid @RequestBody RegisterRequest request) {
		RegistrationResult result = registrationService.register(request.email(), request.password(),
				request.displayName());
		return new RegisterResponse(result.userId(), result.status());
	}

	@PostMapping(AuthPaths.VERIFY_EMAIL)
	public void verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
		emailVerificationService.verify(request.token());
	}
}
