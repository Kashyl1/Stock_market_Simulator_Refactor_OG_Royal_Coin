package com.tradingsimulator.backend.batch.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.tradingsimulator.backend.auth.security.JwtService;
import com.tradingsimulator.backend.auth.security.RestAccessDeniedHandler;
import com.tradingsimulator.backend.auth.security.RestAuthenticationEntryPoint;
import com.tradingsimulator.backend.auth.security.SecurityConfig;
import com.tradingsimulator.backend.batch.BatchItemStatus;
import com.tradingsimulator.backend.batch.BatchJobOrigin;
import com.tradingsimulator.backend.batch.BatchJobStatus;
import com.tradingsimulator.backend.batch.BatchTypeCode;
import com.tradingsimulator.backend.batch.service.BatchAdminService;
import com.tradingsimulator.backend.batch.service.BatchEngineView;
import com.tradingsimulator.backend.batch.service.BatchItemView;
import com.tradingsimulator.backend.batch.service.BatchJobFilter;
import com.tradingsimulator.backend.batch.service.BatchJobView;
import com.tradingsimulator.backend.batch.web.dto.PlanBatchJobRequest;
import com.tradingsimulator.backend.config.CorsProperties;
import com.tradingsimulator.backend.support.TestBatch;
import com.tradingsimulator.backend.web.PageResponse;

import tools.jackson.databind.ObjectMapper;

@WebMvcTest(value = BatchAdminController.class, properties = BatchAdminControllerTest.ALLOWED_ORIGINS_PROPERTY)
@Import(SecurityConfig.class)
@EnableConfigurationProperties(CorsProperties.class)
class BatchAdminControllerTest {

	static final String ALLOWED_ORIGINS_PROPERTY = CorsProperties.PREFIX + ".allowed-origins=http://localhost:4200";

	private static final String ADMIN = "ADMIN";
	private static final String PLANNED_BY = "user:1";
	private static final long JOB_ID = 5L;
	private static final long ITEM_ID = 50L;
	private static final int FIRST_PAGE = 0;
	private static final int PAGE_SIZE = 20;
	private static final int SINGLE = 1;
	private static final int TOO_MANY = PageResponse.MAX_SIZE + 1;
	private static final String SIZE = "size";
	private static final String ITEM_KEY = "USD 2026-10-08";
	private static final BigDecimal USD_MID = new BigDecimal("3.9132");
	private static final String PAYLOAD = "{\"rate\":" + USD_MID + "}";
	private static final String FIRST_JOB_STATUS = "$.content[0].status";
	private static final String FIRST_ITEM_RATE = "$.content[0].payload.rate";
	private static final String STATUS = "$.status";
	private static final String READY_FOR_DEPLOY = "$.readyForDeploy";
	private static final long NOTHING_RUNNING = 0;
	private static final Instant NOW = TestBatch.AT_10_00;

	@Autowired
	private MockMvc mvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private BatchAdminService batch;

	@MockitoBean
	private JwtService jwtService;

	@MockitoBean
	private RestAuthenticationEntryPoint authenticationEntryPoint;

	@MockitoBean
	private RestAccessDeniedHandler accessDeniedHandler;

	@Test
	@WithMockUser(roles = ADMIN)
	void anAdminSeesWhetherTheEngineIsReadyForADeploy() throws Exception {
		BatchEngineView quiet = new BatchEngineView(true, TestBatch.QUIET_FROM, TestBatch.QUIET_UNTIL, ZoneId.of(TestBatch.UTC), NOTHING_RUNNING, true);
		when(batch.engine()).thenReturn(quiet);

		mvc.perform(get(path(BatchAdminPaths.ENGINE))).andExpect(status().isOk()).andExpect(jsonPath(READY_FOR_DEPLOY).value(true));
	}

	@Test
	@WithMockUser(roles = ADMIN)
	void anAdminListsTheJobs() throws Exception {
		when(batch.jobs(any(BatchJobFilter.class), eq(FIRST_PAGE), eq(PAGE_SIZE))).thenReturn(onePage(scheduledJob()));

		mvc.perform(get(path(BatchAdminPaths.JOBS))).andExpect(status().isOk()).andExpect(jsonPath(FIRST_JOB_STATUS).value(BatchJobStatus.SCHEDULED.name()));
	}

	@Test
	@WithMockUser(roles = ADMIN)
	void anItemPayloadIsReturnedAsJson() throws Exception {
		BatchItemView item = new BatchItemView(ITEM_ID, ITEM_KEY, BatchItemStatus.SUCCEEDED, PAYLOAD, null, null, NOW);
		when(batch.items(JOB_ID, null, FIRST_PAGE, PAGE_SIZE)).thenReturn(onePage(item));

		mvc.perform(get(path(BatchAdminPaths.JOB_ITEMS), JOB_ID)).andExpect(status().isOk()).andExpect(jsonPath(FIRST_ITEM_RATE).value(USD_MID.doubleValue()));
	}

	@Test
	@WithMockUser(roles = ADMIN)
	void anAdminPlansAJob() throws Exception {
		when(batch.plan(BatchTypeCode.FX_RATES_NBP, null)).thenReturn(scheduledJob());
		String body = objectMapper.writeValueAsString(new PlanBatchJobRequest(BatchTypeCode.FX_RATES_NBP, null));

		mvc.perform(post(path(BatchAdminPaths.JOBS)).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated())
				.andExpect(jsonPath(STATUS).value(BatchJobStatus.SCHEDULED.name()));
	}

	@Test
	@WithMockUser(roles = ADMIN)
	void anAdminRemovesAPlannedJob() throws Exception {
		mvc.perform(delete(path(BatchAdminPaths.JOB), JOB_ID)).andExpect(status().isNoContent());

		verify(batch).remove(JOB_ID);
	}

	@Test
	@WithMockUser(roles = ADMIN)
	void anAdminStopsARunningJob() throws Exception {
		mvc.perform(post(path(BatchAdminPaths.JOB_STOP), JOB_ID)).andExpect(status().isAccepted());

		verify(batch).stop(JOB_ID);
	}

	@Test
	@WithMockUser(roles = ADMIN)
	void aPageLargerThanTheLimitIsRejected() throws Exception {
		mvc.perform(get(path(BatchAdminPaths.JOBS)).param(SIZE, String.valueOf(TOO_MANY))).andExpect(status().isBadRequest());

		verify(batch, never()).jobs(any(BatchJobFilter.class), anyInt(), anyInt());
	}

	@Test
	@WithMockUser
	void aUserIsDenied() throws Exception {
		mvc.perform(get(path(BatchAdminPaths.JOBS)));

		verify(accessDeniedHandler).handle(any(), any(), any());
		verifyNoInteractions(batch);
	}

	@Test
	void anAnonymousCallerIsSentToTheEntryPoint() throws Exception {
		mvc.perform(get(path(BatchAdminPaths.TYPES)));

		verify(authenticationEntryPoint).commence(any(), any(), any());
		verifyNoInteractions(batch);
	}

	private static BatchJobView scheduledJob() {
		return new BatchJobView(JOB_ID, BatchTypeCode.FX_RATES_NBP, BatchJobStatus.SCHEDULED, BatchJobOrigin.MANUAL, NOW, PLANNED_BY, null, null, null, 0, 0,
				null, null, null, null, null, null);
	}

	private static <T> PageResponse<T> onePage(T element) {
		return new PageResponse<>(List.of(element), FIRST_PAGE, PAGE_SIZE, SINGLE, SINGLE);
	}

	private static String path(String endpoint) {
		return BatchAdminPaths.BASE + endpoint;
	}
}
