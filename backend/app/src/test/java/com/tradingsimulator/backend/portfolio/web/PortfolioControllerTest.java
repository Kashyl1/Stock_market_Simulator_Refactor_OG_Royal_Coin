package com.tradingsimulator.backend.portfolio.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.tradingsimulator.backend.auth.security.JwtService;
import com.tradingsimulator.backend.auth.security.RestAccessDeniedHandler;
import com.tradingsimulator.backend.auth.security.RestAuthenticationEntryPoint;
import com.tradingsimulator.backend.auth.security.SecurityConfig;
import com.tradingsimulator.backend.auth.service.AuthenticatedUser;
import com.tradingsimulator.backend.auth.service.CurrentUserService;
import com.tradingsimulator.backend.config.CorsProperties;
import com.tradingsimulator.backend.portfolio.Performance;
import com.tradingsimulator.backend.portfolio.TransferDirection;
import com.tradingsimulator.backend.portfolio.service.PortfolioService;
import com.tradingsimulator.backend.portfolio.service.PortfolioTransferService;
import com.tradingsimulator.backend.portfolio.service.PortfolioView;
import com.tradingsimulator.backend.portfolio.service.TransferQuoteView;
import com.tradingsimulator.backend.portfolio.service.TransferView;
import com.tradingsimulator.backend.portfolio.web.dto.CreatePortfolioRequest;
import com.tradingsimulator.backend.portfolio.web.dto.TransferRequest;
import com.tradingsimulator.backend.support.TestFx;
import com.tradingsimulator.backend.support.TestMoney;
import com.tradingsimulator.backend.support.TestUsers;

import tools.jackson.databind.ObjectMapper;

@WebMvcTest(value = PortfolioController.class, properties = PortfolioControllerTest.ALLOWED_ORIGINS_PROPERTY)
@Import(SecurityConfig.class)
@EnableConfigurationProperties(CorsProperties.class)
class PortfolioControllerTest {

	static final String ALLOWED_ORIGINS_PROPERTY = CorsProperties.PREFIX + ".allowed-origins=http://localhost:4200";

	private static final BigDecimal FEE = new BigDecimal("2.00");
	private static final String QUOTE_PATH = PortfolioPaths.BASE + PortfolioPaths.TRANSFER_QUOTE;
	private static final String TRANSFERS_PATH = PortfolioPaths.BASE + PortfolioPaths.TRANSFERS;
	private static final String DIRECTION = "direction";
	private static final String AMOUNT = "amount";
	private static final String NAME_PATH = "$.name";
	private static final String FIRST_NAME_PATH = "$[0].name";
	private static final String AMOUNT_PATH = "$.amount";
	private static final String RESULT_PERCENT_PATH = "$.performance.resultPercent";
	private static final BigDecimal RESULT_PERCENT = new BigDecimal("-2.66");
	private static final String BLANK = " ";

	@Autowired
	private MockMvc mvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private PortfolioService portfolios;

	@MockitoBean
	private PortfolioTransferService transfers;

	@MockitoBean
	private CurrentUserService currentUser;

	@MockitoBean
	private JwtService jwtService;

	@MockitoBean
	private RestAuthenticationEntryPoint authenticationEntryPoint;

	@MockitoBean
	private RestAccessDeniedHandler accessDeniedHandler;

	@BeforeEach
	void signInTroodon() {
		when(currentUser.current()).thenReturn(AuthenticatedUser.of(TestUsers.active()));
	}

	@Test
	@WithMockUser
	void listsTheirOwnPortfolios() throws Exception {
		when(portfolios.portfolios(TestUsers.USER_ID)).thenReturn(List.of(portfolio()));

		mvc.perform(get(PortfolioPaths.BASE)).andExpect(status().isOk()).andExpect(jsonPath(FIRST_NAME_PATH).value(TestMoney.PORTFOLIO_NAME));
	}

	@Test
	@WithMockUser
	void createsAPortfolio() throws Exception {
		when(portfolios.create(TestUsers.USER_ID, TestMoney.PORTFOLIO_NAME, TestFx.USD)).thenReturn(portfolio());

		mvc.perform(postJson(PortfolioPaths.BASE, new CreatePortfolioRequest(TestMoney.PORTFOLIO_NAME, TestFx.USD))).andExpect(status().isCreated())
				.andExpect(jsonPath(NAME_PATH).value(TestMoney.PORTFOLIO_NAME)).andExpect(jsonPath(RESULT_PERCENT_PATH).value(RESULT_PERCENT.doubleValue()));
	}

	@Test
	@WithMockUser
	void refusesAPortfolioWithoutAName() throws Exception {
		mvc.perform(postJson(PortfolioPaths.BASE, new CreatePortfolioRequest(BLANK, TestFx.USD))).andExpect(status().isBadRequest());

		verifyNoInteractions(portfolios);
	}

	@Test
	@WithMockUser
	void quotesATransferWithoutMakingIt() throws Exception {
		TransferQuoteView quote = new TransferQuoteView(TransferDirection.IN, TestMoney.ARRIVED_DOLLARS, TestFx.USD, TestMoney.DEPOSITED_ZLOTY, TestFx.PLN,
				TestMoney.DOLLARS_PER_ZLOTY, FEE);
		when(transfers.quote(TestUsers.USER_ID, TestMoney.PORTFOLIO_ID, TransferDirection.IN, TestMoney.DEPOSITED_ZLOTY)).thenReturn(quote);
		String amount = TestMoney.DEPOSITED_ZLOTY.toPlainString();

		mvc.perform(get(QUOTE_PATH, TestMoney.PORTFOLIO_ID).param(DIRECTION, TransferDirection.IN.name()).param(AMOUNT, amount)).andExpect(status().isOk())
				.andExpect(jsonPath(AMOUNT_PATH).value(TestMoney.ARRIVED_DOLLARS.doubleValue()));
	}

	@Test
	@WithMockUser
	void transfersMoneyIntoAPortfolio() throws Exception {
		when(transfers.transfer(TestUsers.USER_ID, TestMoney.PORTFOLIO_ID, TransferDirection.IN, TestMoney.DEPOSITED_ZLOTY)).thenReturn(transfer());

		TransferRequest request = new TransferRequest(TransferDirection.IN, TestMoney.DEPOSITED_ZLOTY);

		mvc.perform(postJson(TRANSFERS_PATH, request, TestMoney.PORTFOLIO_ID)).andExpect(status().isCreated())
				.andExpect(jsonPath(AMOUNT_PATH).value(TestMoney.ARRIVED_DOLLARS.doubleValue()));
	}

	@Test
	@WithMockUser
	void refusesANegativeAmount() throws Exception {
		mvc.perform(postJson(TRANSFERS_PATH, new TransferRequest(TransferDirection.IN, TestMoney.DEPOSITED_ZLOTY.negate()), TestMoney.PORTFOLIO_ID))
				.andExpect(status().isBadRequest());

		verifyNoInteractions(transfers);
	}

	@Test
	void anAnonymousCallerIsSentToTheEntryPoint() throws Exception {
		mvc.perform(get(PortfolioPaths.BASE));

		verify(authenticationEntryPoint).commence(any(), any(), any());
		verifyNoInteractions(portfolios);
	}

	private MockHttpServletRequestBuilder postJson(String path, Object body, Object... pathVariables) {
		return post(path, pathVariables).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body));
	}

	private static PortfolioView portfolio() {
		return TestMoney.dollarPortfolio(Performance.of(TestFx.PLN, TestMoney.DOLLARS_IN_ZLOTY, TestMoney.DEPOSITED_ZLOTY, BigDecimal.ZERO));
	}

	private static TransferView transfer() {
		return new TransferView(TestMoney.TRANSFER_ID, TransferDirection.IN, TestMoney.ARRIVED_DOLLARS, TestFx.USD, TestMoney.DEPOSITED_ZLOTY, TestFx.PLN,
				TestMoney.DOLLARS_PER_ZLOTY, FEE, TestMoney.NOW);
	}
}
