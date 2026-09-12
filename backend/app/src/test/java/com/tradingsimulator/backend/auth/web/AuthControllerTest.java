package com.tradingsimulator.backend.auth.web;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.tradingsimulator.backend.auth.AuthError;
import com.tradingsimulator.backend.auth.AuthPaths;
import com.tradingsimulator.backend.auth.UserStatus;
import com.tradingsimulator.backend.auth.security.JwtService;
import com.tradingsimulator.backend.auth.security.RestAccessDeniedHandler;
import com.tradingsimulator.backend.auth.security.RestAuthenticationEntryPoint;
import com.tradingsimulator.backend.auth.security.SecurityConfig;
import com.tradingsimulator.backend.auth.service.EmailVerificationService;
import com.tradingsimulator.backend.auth.service.RegistrationResult;
import com.tradingsimulator.backend.auth.service.RegistrationService;
import com.tradingsimulator.backend.auth.web.dto.RegisterRequest;
import com.tradingsimulator.backend.config.CorsProperties;
import com.tradingsimulator.backend.support.TestUsers;
import com.tradingsimulator.backend.auth.web.dto.VerifyEmailRequest;
import com.tradingsimulator.backend.common.error.AppException;
import com.tradingsimulator.backend.common.error.CommonError;

import tools.jackson.databind.ObjectMapper;

@WebMvcTest(value = AuthController.class, properties = AuthControllerTest.ALLOWED_ORIGINS_PROPERTY)
@Import(SecurityConfig.class)
@EnableConfigurationProperties(CorsProperties.class)
class AuthControllerTest {

	static final String ALLOWED_ORIGINS_PROPERTY = CorsProperties.PREFIX + ".allowed-origins=http://localhost:4200";

	private static final String EMAIL = TestUsers.EMAIL;
	private static final String PASSWORD = TestUsers.PASSWORD;
	private static final String DISPLAY_NAME = TestUsers.DISPLAY_NAME;
	private static final String RAW_TOKEN = "raw-token";
	private static final long USER_ID = TestUsers.USER_ID;
	private static final String EMAIL_FIELD = "email";
	private static final String NOT_AN_EMAIL = "not-an-email";
	private static final String CODE = "$.code";
	private static final String USER_ID_PATH = "$.userId";
	private static final String STATUS_PATH = "$.status";
	private static final String FIRST_FIELD_ERROR = "$.fieldErrors[0].field";

	@Autowired
	private MockMvc mvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private RegistrationService registrationService;

	@MockitoBean
	private EmailVerificationService emailVerificationService;

	@MockitoBean
	private JwtService jwtService;

	@MockitoBean
	private RestAuthenticationEntryPoint authenticationEntryPoint;

	@MockitoBean
	private RestAccessDeniedHandler accessDeniedHandler;

	@Test
	void registersWithoutATokenAndReturnsTheNewUser() throws Exception {
		when(registrationService.register(EMAIL, PASSWORD, DISPLAY_NAME))
				.thenReturn(new RegistrationResult(USER_ID, UserStatus.PENDING_VERIFICATION));

		mvc.perform(postJson(AuthPaths.BASE + AuthPaths.REGISTER,
				new RegisterRequest(EMAIL, PASSWORD, DISPLAY_NAME)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath(USER_ID_PATH).value(USER_ID))
				.andExpect(jsonPath(STATUS_PATH).value(UserStatus.PENDING_VERIFICATION.name()));
	}

	@Test
	void reportsAnInvalidEmailAsAFieldError() throws Exception {
		mvc.perform(postJson(AuthPaths.BASE + AuthPaths.REGISTER,
				new RegisterRequest(NOT_AN_EMAIL, PASSWORD, DISPLAY_NAME)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath(CODE).value(CommonError.VALIDATION_FAILED.code()))
				.andExpect(jsonPath(FIRST_FIELD_ERROR).value(EMAIL_FIELD));
	}

	@Test
	void reportsAnAlreadyRegisteredEmailAsAConflict() throws Exception {
		when(registrationService.register(EMAIL, PASSWORD, DISPLAY_NAME))
				.thenThrow(new AppException(AuthError.EMAIL_ALREADY_REGISTERED, EMAIL));

		mvc.perform(postJson(AuthPaths.BASE + AuthPaths.REGISTER,
				new RegisterRequest(EMAIL, PASSWORD, DISPLAY_NAME)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath(CODE).value(AuthError.EMAIL_ALREADY_REGISTERED.code()));
	}

	@Test
	void verifiesAnEmailWithoutAToken() throws Exception {
		mvc.perform(postJson(AuthPaths.BASE + AuthPaths.VERIFY_EMAIL, new VerifyEmailRequest(RAW_TOKEN)))
				.andExpect(status().isOk());

		verify(emailVerificationService).verify(RAW_TOKEN);
	}

	@Test
	void reportsAnInvalidVerificationToken() throws Exception {
		doThrow(new AppException(AuthError.VERIFICATION_TOKEN_INVALID))
				.when(emailVerificationService).verify(anyString());

		mvc.perform(postJson(AuthPaths.BASE + AuthPaths.VERIFY_EMAIL, new VerifyEmailRequest(RAW_TOKEN)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath(CODE).value(AuthError.VERIFICATION_TOKEN_INVALID.code()));
	}

	private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder postJson(String path,
			Object body) {
		return post(path).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body));
	}
}
