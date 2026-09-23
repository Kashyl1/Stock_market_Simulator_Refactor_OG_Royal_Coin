package com.tradingsimulator.backend.auth.web;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.tradingsimulator.backend.auth.AuthError;
import com.tradingsimulator.backend.auth.AuthPaths;
import com.tradingsimulator.backend.auth.Role;
import com.tradingsimulator.backend.auth.UserStatus;
import com.tradingsimulator.backend.auth.security.JwtService;
import com.tradingsimulator.backend.auth.security.RestAccessDeniedHandler;
import com.tradingsimulator.backend.auth.security.RestAuthenticationEntryPoint;
import com.tradingsimulator.backend.auth.security.SecurityConfig;
import com.tradingsimulator.backend.auth.service.AuthTokens;
import com.tradingsimulator.backend.auth.service.AuthenticatedUser;
import com.tradingsimulator.backend.auth.service.CurrentUserService;
import com.tradingsimulator.backend.auth.service.EmailVerificationService;
import com.tradingsimulator.backend.auth.service.LoginResult;
import com.tradingsimulator.backend.auth.service.LoginService;
import com.tradingsimulator.backend.auth.service.PasswordResetService;
import com.tradingsimulator.backend.auth.service.RefreshTokenService;
import com.tradingsimulator.backend.auth.service.RegistrationResult;
import com.tradingsimulator.backend.auth.service.RegistrationService;
import com.tradingsimulator.backend.auth.web.dto.ForgotPasswordRequest;
import com.tradingsimulator.backend.auth.web.dto.LoginRequest;
import com.tradingsimulator.backend.auth.web.dto.RegisterRequest;
import com.tradingsimulator.backend.auth.web.dto.ResetPasswordRequest;
import com.tradingsimulator.backend.auth.web.dto.VerifyEmailRequest;
import com.tradingsimulator.backend.common.error.AppException;
import com.tradingsimulator.backend.common.error.CommonError;
import com.tradingsimulator.backend.config.CorsProperties;
import com.tradingsimulator.backend.support.TestAuthProperties;
import com.tradingsimulator.backend.support.TestUsers;

import tools.jackson.databind.ObjectMapper;

@WebMvcTest(value = AuthController.class, properties = AuthControllerTest.ALLOWED_ORIGINS_PROPERTY)
@Import(SecurityConfig.class)
@EnableConfigurationProperties(CorsProperties.class)
class AuthControllerTest {

	static final String ALLOWED_ORIGINS_PROPERTY = CorsProperties.PREFIX + ".allowed-origins=http://localhost:4200";

	private static final String RAW_TOKEN = "raw-token";
	private static final String ACCESS_TOKEN = "access-token";
	private static final String REFRESH_TOKEN = "refresh-token";
	private static final String ROTATED_REFRESH_TOKEN = "rotated-refresh-token";
	private static final String NEW_PASSWORD = "brand-new-password";
	private static final String EMAIL_FIELD = "email";
	private static final String NOT_AN_EMAIL = "not-an-email";
	private static final String CODE = "$.code";
	private static final String USER_ID_PATH = "$.userId";
	private static final String STATUS_PATH = "$.status";
	private static final String ACCESS_TOKEN_PATH = "$.accessToken";
	private static final String EXPIRES_IN_PATH = "$.expiresInSeconds";
	private static final String USER_EMAIL_PATH = "$.user.email";
	private static final String ID_PATH = "$.id";
	private static final String EMAIL_PATH = "$.email";
	private static final String FIRST_FIELD_ERROR = "$.fieldErrors[0].field";
	private static final String HTTP_ONLY = "HttpOnly";
	private static final String SAME_SITE_STRICT = "SameSite=Strict";
	private static final String CLEARED_COOKIE = RefreshTokenCookie.NAME + "=;";

	@Autowired
	private MockMvc mvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private RegistrationService registrationService;

	@MockitoBean
	private EmailVerificationService emailVerificationService;

	@MockitoBean
	private LoginService loginService;

	@MockitoBean
	private RefreshTokenService refreshTokenService;

	@MockitoBean
	private CurrentUserService currentUserService;

	@MockitoBean
	private PasswordResetService passwordResetService;

	@MockitoBean
	private JwtService jwtService;

	@MockitoBean
	private RestAuthenticationEntryPoint authenticationEntryPoint;

	@MockitoBean
	private RestAccessDeniedHandler accessDeniedHandler;

	@Test
	void registersWithoutATokenAndReturnsTheNewUser() throws Exception {
		when(registrationService.register(TestUsers.EMAIL, TestUsers.PASSWORD, TestUsers.DISPLAY_NAME))
				.thenReturn(new RegistrationResult(TestUsers.USER_ID, UserStatus.PENDING_VERIFICATION));

		mvc.perform(postJson(AuthPaths.REGISTER, new RegisterRequest(TestUsers.EMAIL, TestUsers.PASSWORD, TestUsers.DISPLAY_NAME)))
				.andExpect(status().isCreated()).andExpect(jsonPath(USER_ID_PATH).value(TestUsers.USER_ID))
				.andExpect(jsonPath(STATUS_PATH).value(UserStatus.PENDING_VERIFICATION.name()));
	}

	@Test
	void reportsAnInvalidEmailAsAFieldError() throws Exception {
		mvc.perform(postJson(AuthPaths.REGISTER, new RegisterRequest(NOT_AN_EMAIL, TestUsers.PASSWORD, TestUsers.DISPLAY_NAME)))
				.andExpect(status().isBadRequest()).andExpect(jsonPath(CODE).value(CommonError.VALIDATION_FAILED.code()))
				.andExpect(jsonPath(FIRST_FIELD_ERROR).value(EMAIL_FIELD));
	}

	@Test
	void reportsAnAlreadyRegisteredEmailAsAConflict() throws Exception {
		when(registrationService.register(TestUsers.EMAIL, TestUsers.PASSWORD, TestUsers.DISPLAY_NAME))
				.thenThrow(new AppException(AuthError.EMAIL_ALREADY_REGISTERED, TestUsers.EMAIL));

		mvc.perform(postJson(AuthPaths.REGISTER, new RegisterRequest(TestUsers.EMAIL, TestUsers.PASSWORD, TestUsers.DISPLAY_NAME)))
				.andExpect(status().isConflict()).andExpect(jsonPath(CODE).value(AuthError.EMAIL_ALREADY_REGISTERED.code()));
	}

	@Test
	void verifiesAnEmailWithoutAToken() throws Exception {
		mvc.perform(postJson(AuthPaths.VERIFY_EMAIL, new VerifyEmailRequest(RAW_TOKEN))).andExpect(status().isOk());

		verify(emailVerificationService).verify(RAW_TOKEN);
	}

	@Test
	void reportsAnInvalidVerificationToken() throws Exception {
		doThrow(new AppException(AuthError.VERIFICATION_TOKEN_INVALID)).when(emailVerificationService).verify(anyString());

		mvc.perform(postJson(AuthPaths.VERIFY_EMAIL, new VerifyEmailRequest(RAW_TOKEN))).andExpect(status().isBadRequest())
				.andExpect(jsonPath(CODE).value(AuthError.VERIFICATION_TOKEN_INVALID.code()));
	}

	@Test
	void logsInAndSetsTheRefreshCookie() throws Exception {
		when(loginService.login(anyString(), anyString(), any())).thenReturn(new LoginResult(tokens(REFRESH_TOKEN), authenticatedUser()));

		mvc.perform(postJson(AuthPaths.LOGIN, new LoginRequest(TestUsers.EMAIL, TestUsers.PASSWORD))).andExpect(status().isOk())
				.andExpect(jsonPath(ACCESS_TOKEN_PATH).value(ACCESS_TOKEN))
				.andExpect(jsonPath(EXPIRES_IN_PATH).value(TestAuthProperties.ACCESS_TOKEN_TTL.toSeconds()))
				.andExpect(jsonPath(USER_EMAIL_PATH).value(TestUsers.EMAIL))
				.andExpect(header().string(HttpHeaders.SET_COOKIE, containsString(cookieValue(REFRESH_TOKEN))))
				.andExpect(header().string(HttpHeaders.SET_COOKIE, containsString(HTTP_ONLY)))
				.andExpect(header().string(HttpHeaders.SET_COOKIE, containsString(SAME_SITE_STRICT)));
	}

	@Test
	void reportsWrongCredentialsAsUnauthorized() throws Exception {
		when(loginService.login(anyString(), anyString(), any())).thenThrow(new AppException(AuthError.INVALID_CREDENTIALS));

		mvc.perform(postJson(AuthPaths.LOGIN, new LoginRequest(TestUsers.EMAIL, TestUsers.PASSWORD))).andExpect(status().isUnauthorized())
				.andExpect(jsonPath(CODE).value(AuthError.INVALID_CREDENTIALS.code()));
	}

	@Test
	void rotatesTheRefreshCookieOnRefresh() throws Exception {
		when(refreshTokenService.rotate(anyString(), any())).thenReturn(tokens(ROTATED_REFRESH_TOKEN));

		mvc.perform(post(path(AuthPaths.REFRESH)).cookie(refreshCookie())).andExpect(status().isOk())
				.andExpect(jsonPath(ACCESS_TOKEN_PATH).value(ACCESS_TOKEN)).andExpect(header().string(HttpHeaders.SET_COOKIE,
						containsString(cookieValue(ROTATED_REFRESH_TOKEN))));
	}

	@Test
	void refusesToRefreshWithoutACookie() throws Exception {
		mvc.perform(post(path(AuthPaths.REFRESH))).andExpect(status().isUnauthorized())
				.andExpect(jsonPath(CODE).value(AuthError.REFRESH_TOKEN_INVALID.code()));
	}

	@Test
	void endsTheSessionAndClearsTheCookieOnLogout() throws Exception {
		mvc.perform(post(path(AuthPaths.LOGOUT)).cookie(refreshCookie())).andExpect(status().isNoContent())
				.andExpect(header().string(HttpHeaders.SET_COOKIE, containsString(CLEARED_COOKIE)));

		verify(refreshTokenService).revoke(REFRESH_TOKEN);
	}

	@Test
	@WithMockUser
	void returnsTheAccountBehindTheAccessToken() throws Exception {
		when(currentUserService.current()).thenReturn(authenticatedUser());

		mvc.perform(get(path(AuthPaths.ME))).andExpect(status().isOk()).andExpect(jsonPath(ID_PATH).value(TestUsers.USER_ID))
				.andExpect(jsonPath(EMAIL_PATH).value(TestUsers.EMAIL));
	}

	@Test
	void sendsAnUnauthenticatedCallToTheEntryPoint() throws Exception {
		mvc.perform(get(path(AuthPaths.ME)));

		verify(authenticationEntryPoint).commence(any(), any(), any());
	}

	@Test
	void acceptsAForgottenPasswordWithoutSayingWhetherTheAccountExists() throws Exception {
		mvc.perform(postJson(AuthPaths.FORGOT_PASSWORD, new ForgotPasswordRequest(TestUsers.OTHER_EMAIL))).andExpect(status().isAccepted());

		verify(passwordResetService).requestReset(TestUsers.OTHER_EMAIL);
	}

	@Test
	void resetsThePassword() throws Exception {
		mvc.perform(postJson(AuthPaths.RESET_PASSWORD, new ResetPasswordRequest(RAW_TOKEN, NEW_PASSWORD))).andExpect(status().isOk());

		verify(passwordResetService).reset(RAW_TOKEN, NEW_PASSWORD);
	}

	@Test
	void reportsAnExpiredResetToken() throws Exception {
		doThrow(new AppException(AuthError.RESET_TOKEN_EXPIRED)).when(passwordResetService).reset(anyString(), anyString());

		mvc.perform(postJson(AuthPaths.RESET_PASSWORD, new ResetPasswordRequest(RAW_TOKEN, NEW_PASSWORD))).andExpect(status().isBadRequest())
				.andExpect(jsonPath(CODE).value(AuthError.RESET_TOKEN_EXPIRED.code()));
	}

	private static String path(String endpoint) {
		return AuthPaths.BASE + endpoint;
	}

	private static String cookieValue(String rawToken) {
		return RefreshTokenCookie.NAME + "=" + rawToken;
	}

	private static Cookie refreshCookie() {
		return new Cookie(RefreshTokenCookie.NAME, REFRESH_TOKEN);
	}

	private static AuthTokens tokens(String refreshToken) {
		return new AuthTokens(ACCESS_TOKEN, TestAuthProperties.ACCESS_TOKEN_TTL, refreshToken, TestAuthProperties.REFRESH_TOKEN_TTL);
	}

	private static AuthenticatedUser authenticatedUser() {
		return new AuthenticatedUser(TestUsers.USER_ID, TestUsers.EMAIL, TestUsers.DISPLAY_NAME, Role.USER, UserStatus.ACTIVE);
	}

	private MockHttpServletRequestBuilder postJson(String endpoint, Object body) {
		return post(path(endpoint)).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body));
	}
}
