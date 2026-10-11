package com.tradingsimulator.backend.account.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.tradingsimulator.backend.account.service.AccountEntryView;
import com.tradingsimulator.backend.account.service.AccountService;
import com.tradingsimulator.backend.account.service.AccountView;
import com.tradingsimulator.backend.auth.security.JwtService;
import com.tradingsimulator.backend.auth.security.RestAccessDeniedHandler;
import com.tradingsimulator.backend.auth.security.RestAuthenticationEntryPoint;
import com.tradingsimulator.backend.auth.security.SecurityConfig;
import com.tradingsimulator.backend.auth.service.AuthenticatedUser;
import com.tradingsimulator.backend.auth.service.CurrentUserService;
import com.tradingsimulator.backend.config.CorsProperties;
import com.tradingsimulator.backend.portfolio.Performance;
import com.tradingsimulator.backend.support.TestFx;
import com.tradingsimulator.backend.support.TestMoney;
import com.tradingsimulator.backend.support.TestUsers;
import com.tradingsimulator.backend.wallet.WalletEntryType;
import com.tradingsimulator.backend.web.PageResponse;

@WebMvcTest(value = AccountController.class, properties = AccountControllerTest.ALLOWED_ORIGINS_PROPERTY)
@Import(SecurityConfig.class)
@EnableConfigurationProperties(CorsProperties.class)
class AccountControllerTest {

	static final String ALLOWED_ORIGINS_PROPERTY = CorsProperties.PREFIX + ".allowed-origins=http://localhost:4200";

	private static final int FIRST_PAGE = 0;
	private static final int PAGE_SIZE = 20;
	private static final int SINGLE = 1;
	private static final long ENTRY_ID = 1L;
	private static final String CURRENCY_PATH = "$.currency";
	private static final String VALUE_PATH = "$.performance.value";
	private static final String FIRST_TYPE_PATH = "$.content[0].type";

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private AccountService accounts;

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
	void showsTheMainAccountWithItsValue() throws Exception {
		Performance performance = Performance.of(TestFx.PLN, TestMoney.STARTING_BALANCE, TestMoney.STARTING_BALANCE, BigDecimal.ZERO);
		when(accounts.account(TestUsers.USER_ID)).thenReturn(new AccountView(TestFx.PLN, TestMoney.STARTING_BALANCE, TestMoney.STARTING_BALANCE, performance));

		mvc.perform(get(AccountPaths.BASE)).andExpect(status().isOk()).andExpect(jsonPath(CURRENCY_PATH).value(TestFx.PLN))
				.andExpect(jsonPath(VALUE_PATH).value(TestMoney.STARTING_BALANCE.doubleValue()));
	}

	@Test
	@WithMockUser
	void showsTheHistoryOfTheMainAccount() throws Exception {
		AccountEntryView deposit = new AccountEntryView(ENTRY_ID, WalletEntryType.DEPOSIT, TestMoney.STARTING_BALANCE, TestFx.PLN, TestMoney.STARTING_BALANCE,
				TestMoney.NOW, null, null);
		PageResponse<AccountEntryView> history = new PageResponse<>(List.of(deposit), FIRST_PAGE, PAGE_SIZE, SINGLE, SINGLE);
		when(accounts.entries(TestUsers.USER_ID, FIRST_PAGE, PAGE_SIZE)).thenReturn(history);

		mvc.perform(get(AccountPaths.BASE + AccountPaths.ENTRIES)).andExpect(status().isOk())
				.andExpect(jsonPath(FIRST_TYPE_PATH).value(WalletEntryType.DEPOSIT.name()));
	}

	@Test
	void anAnonymousCallerIsSentToTheEntryPoint() throws Exception {
		mvc.perform(get(AccountPaths.BASE));

		verify(authenticationEntryPoint).commence(any(), any(), any());
		verifyNoInteractions(accounts);
	}
}
