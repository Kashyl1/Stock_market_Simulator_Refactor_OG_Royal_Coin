package com.tradingsimulator.backend.common.error;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

class GlobalExceptionHandlerTest {

	private static final String PROBE = "/probe";
	private static final String MISSING = "/missing";
	private static final String ITEMS = "/items";
	private static final String BOOM = "/boom";
	private static final String FORBIDDEN = "/forbidden";
	private static final String CODE = "$.code";
	private static final String PATH = "$.path";
	private static final String MESSAGE = "$.message";
	private static final String FIRST_FIELD_ERROR = "$.fieldErrors[0].field";
	private static final String NAME_FIELD = "name";
	private static final String MALFORMED_JSON = "{not json";
	private static final String BLANK_NAME_JSON = "{\"" + NAME_FIELD + "\":\"\"}";
	private static final String MISSING_RESOURCE = "item";
	private static final long MISSING_ID = 7L;
	private static final String INTERNAL_DETAIL = "internal detail";
	private static final String DENIED = "denied";

	private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new ProbeController())
			.setControllerAdvice(new GlobalExceptionHandler())
			.build();

	@Test
	void mapsAnAppExceptionToItsCodeAndStatus() throws Exception {
		mvc.perform(get(PROBE + MISSING))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath(CODE).value(CommonError.RESOURCE_NOT_FOUND.code()))
				.andExpect(jsonPath(PATH).value(PROBE + MISSING));
	}

	@Test
	void keepsBadRequestForAMalformedJsonBody() throws Exception {
		mvc.perform(post(PROBE + ITEMS).contentType(MediaType.APPLICATION_JSON).content(MALFORMED_JSON))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath(CODE).value(CommonError.MALFORMED_REQUEST.code()));
	}

	@Test
	void keepsMethodNotAllowedForAnUnsupportedMethod() throws Exception {
		mvc.perform(delete(PROBE + ITEMS))
				.andExpect(status().isMethodNotAllowed())
				.andExpect(jsonPath(CODE).value(CommonError.METHOD_NOT_ALLOWED.code()));
	}

	@Test
	void reportsFieldErrorsForAnInvalidBody() throws Exception {
		mvc.perform(post(PROBE + ITEMS).contentType(MediaType.APPLICATION_JSON).content(BLANK_NAME_JSON))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath(CODE).value(CommonError.VALIDATION_FAILED.code()))
				.andExpect(jsonPath(FIRST_FIELD_ERROR).value(NAME_FIELD));
	}

	@Test
	void hidesTheDetailOfAnUnexpectedFailure() throws Exception {
		mvc.perform(get(PROBE + BOOM))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath(CODE).value(CommonError.INTERNAL_ERROR.code()))
				.andExpect(jsonPath(MESSAGE).value(CommonError.INTERNAL_ERROR.messageTemplate()))
				.andExpect(content().string(not(containsString(INTERNAL_DETAIL))));
	}

	@Test
	void leavesAccessDeniedToSpringSecurity() {
		assertThatThrownBy(() -> mvc.perform(get(PROBE + FORBIDDEN)))
				.satisfies(thrown -> assertThat(causalChain(thrown))
						.hasAtLeastOneElementOfType(AccessDeniedException.class));
	}

	private static List<Throwable> causalChain(Throwable thrown) {
		List<Throwable> chain = new ArrayList<>();
		for (Throwable current = thrown; current != null; current = current.getCause()) {
			chain.add(current);
		}
		return chain;
	}

	@RestController
	@RequestMapping(PROBE)
	static class ProbeController {

		@GetMapping(MISSING)
		void missing() {
			throw new AppException(CommonError.RESOURCE_NOT_FOUND, MISSING_RESOURCE, MISSING_ID);
		}

		@PostMapping(ITEMS)
		void create(@Valid @RequestBody ProbeItem item) {
		}

		@GetMapping(BOOM)
		void boom() {
			throw new IllegalStateException(INTERNAL_DETAIL);
		}

		@GetMapping(FORBIDDEN)
		void forbidden() {
			throw new AccessDeniedException(DENIED);
		}
	}

	record ProbeItem(@NotBlank String name) {
	}
}
