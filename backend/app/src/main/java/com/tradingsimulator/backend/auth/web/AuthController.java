package com.tradingsimulator.backend.auth.web;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.tradingsimulator.backend.auth.AuthError;
import com.tradingsimulator.backend.auth.AuthPaths;
import com.tradingsimulator.backend.auth.service.AuthTokens;
import com.tradingsimulator.backend.auth.service.ClientDetails;
import com.tradingsimulator.backend.auth.service.CurrentUserService;
import com.tradingsimulator.backend.auth.service.EmailVerificationService;
import com.tradingsimulator.backend.auth.service.LoginResult;
import com.tradingsimulator.backend.auth.service.LoginService;
import com.tradingsimulator.backend.auth.service.PasswordResetService;
import com.tradingsimulator.backend.auth.service.RefreshTokenService;
import com.tradingsimulator.backend.auth.service.RegistrationResult;
import com.tradingsimulator.backend.auth.service.RegistrationService;
import com.tradingsimulator.backend.auth.web.dto.AccessTokenResponse;
import com.tradingsimulator.backend.auth.web.dto.CurrentUserResponse;
import com.tradingsimulator.backend.auth.web.dto.ForgotPasswordRequest;
import com.tradingsimulator.backend.auth.web.dto.LoginRequest;
import com.tradingsimulator.backend.auth.web.dto.LoginResponse;
import com.tradingsimulator.backend.auth.web.dto.RegisterRequest;
import com.tradingsimulator.backend.auth.web.dto.RegisterResponse;
import com.tradingsimulator.backend.auth.web.dto.ResetPasswordRequest;
import com.tradingsimulator.backend.auth.web.dto.VerifyEmailRequest;
import com.tradingsimulator.backend.common.error.AppException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(AuthPaths.BASE)
@RequiredArgsConstructor
public class AuthController {

	private final RegistrationService registrationService;
	private final EmailVerificationService emailVerificationService;
	private final LoginService loginService;
	private final RefreshTokenService refreshTokenService;
	private final CurrentUserService currentUserService;
	private final PasswordResetService passwordResetService;

	@PostMapping(AuthPaths.REGISTER)
	@ResponseStatus(HttpStatus.CREATED)
	public RegisterResponse register(@Valid @RequestBody RegisterRequest request) {
		RegistrationResult result = registrationService.register(request.email(), request.password(), request.displayName());
		return new RegisterResponse(result.userId(), result.status());
	}

	@PostMapping(AuthPaths.VERIFY_EMAIL)
	public void verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
		emailVerificationService.verify(request.token());
	}

	@PostMapping(AuthPaths.LOGIN)
	public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
		LoginResult result = loginService.login(request.email(), request.password(), clientOf(httpRequest));
		LoginResponse body = new LoginResponse(result.tokens().accessToken(), expiresInSeconds(result.tokens()), CurrentUserResponse.of(result.user()));
		return withRefreshCookie(result.tokens()).body(body);
	}

	@PostMapping(AuthPaths.REFRESH)
	public ResponseEntity<AccessTokenResponse> refresh(@CookieValue(name = RefreshTokenCookie.NAME, required = false) String refreshToken,
			HttpServletRequest httpRequest) {
		AuthTokens tokens = refreshTokenService.rotate(presented(refreshToken), clientOf(httpRequest));
		return withRefreshCookie(tokens).body(new AccessTokenResponse(tokens.accessToken(), expiresInSeconds(tokens)));
	}

	@PostMapping(AuthPaths.LOGOUT)
	public ResponseEntity<Void> logout(@CookieValue(name = RefreshTokenCookie.NAME, required = false) String refreshToken) {
		if (refreshToken != null && !refreshToken.isBlank()) {
			refreshTokenService.revoke(refreshToken);
		}
		return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, RefreshTokenCookie.cleared().toString()).build();
	}

	@GetMapping(AuthPaths.ME)
	public CurrentUserResponse me() {
		return CurrentUserResponse.of(currentUserService.current());
	}

	@PostMapping(AuthPaths.FORGOT_PASSWORD)
	@ResponseStatus(HttpStatus.ACCEPTED)
	public void forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
		passwordResetService.requestReset(request.email());
	}

	@PostMapping(AuthPaths.RESET_PASSWORD)
	public void resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
		passwordResetService.reset(request.token(), request.newPassword());
	}

	private static ClientDetails clientOf(HttpServletRequest request) {
		return new ClientDetails(request.getHeader(HttpHeaders.USER_AGENT), request.getRemoteAddr());
	}

	private static String presented(String refreshToken) {
		if (refreshToken == null || refreshToken.isBlank()) {
			throw new AppException(AuthError.REFRESH_TOKEN_INVALID);
		}
		return refreshToken;
	}

	private static ResponseEntity.BodyBuilder withRefreshCookie(AuthTokens tokens) {
		return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, RefreshTokenCookie.of(tokens.refreshToken(), tokens.refreshTokenTtl()).toString());
	}

	private static long expiresInSeconds(AuthTokens tokens) {
		return tokens.accessTokenTtl().toSeconds();
	}
}
