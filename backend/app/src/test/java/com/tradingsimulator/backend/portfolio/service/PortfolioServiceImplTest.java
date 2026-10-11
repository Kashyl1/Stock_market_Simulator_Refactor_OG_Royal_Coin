package com.tradingsimulator.backend.portfolio.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.tradingsimulator.backend.common.error.AppException;
import com.tradingsimulator.backend.common.error.ErrorCode;
import com.tradingsimulator.backend.currency.CurrencyRepository;
import com.tradingsimulator.backend.currency.FxError;
import com.tradingsimulator.backend.currency.service.FxConversionService;
import com.tradingsimulator.backend.portfolio.Portfolio;
import com.tradingsimulator.backend.portfolio.PortfolioError;
import com.tradingsimulator.backend.portfolio.PortfolioRepository;
import com.tradingsimulator.backend.portfolio.PortfolioTransferRepository;
import com.tradingsimulator.backend.portfolio.TransferDirection;
import com.tradingsimulator.backend.portfolio.TransferTotals;
import com.tradingsimulator.backend.support.TestEntities;
import com.tradingsimulator.backend.support.TestFx;
import com.tradingsimulator.backend.support.TestMoney;
import com.tradingsimulator.backend.support.TestUsers;
import com.tradingsimulator.backend.wallet.Wallet;
import com.tradingsimulator.backend.wallet.WalletRepository;

class PortfolioServiceImplTest {

	private static final String UNKNOWN_CURRENCY = "XYZ";
	private static final String SPACES = "  ";
	private static final String NAME_WITH_SPACES = SPACES + TestMoney.PORTFOLIO_NAME + SPACES;
	private static final List<Long> PORTFOLIO_IDS = List.of(TestMoney.PORTFOLIO_ID);

	private final PortfolioRepository portfolios = mock(PortfolioRepository.class);
	private final WalletRepository wallets = mock(WalletRepository.class);
	private final PortfolioTransferRepository transfers = mock(PortfolioTransferRepository.class);
	private final CurrencyRepository currencies = mock(CurrencyRepository.class);
	private final FxConversionService fx = mock(FxConversionService.class);

	private final PortfolioService service = new PortfolioServiceImpl(portfolios, wallets, transfers, currencies, fx, TestFx.properties(), TestMoney.CLOCK);

	@BeforeEach
	void stubAccount() {
		when(currencies.supported(TestFx.PLN)).thenReturn(TestFx.currency(TestFx.PLN, TestFx.CENTS));
		when(currencies.supported(TestFx.USD)).thenReturn(TestFx.currency(TestFx.USD, TestFx.CENTS));
		when(wallets.mainAccountOf(TestUsers.USER_ID)).thenReturn(TestMoney.mainAccount(TestMoney.STARTING_BALANCE));
		when(fx.findRate(TestFx.USD, TestFx.PLN, TestFx.THURSDAY)).thenReturn(Optional.of(TestFx.USD_MID));
	}

	@Test
	void createsAPortfolioWithAnEmptyWalletInItsCurrency() {
		when(portfolios.saveAndFlush(any(Portfolio.class))).thenAnswer(invocation -> TestEntities.withId(invocation.getArgument(0), TestMoney.PORTFOLIO_ID));
		givenPortfolioWallet(BigDecimal.ZERO, List.of());

		PortfolioView created = service.create(TestUsers.USER_ID, NAME_WITH_SPACES, TestFx.USD);

		ArgumentCaptor<Wallet> wallet = ArgumentCaptor.forClass(Wallet.class);
		verify(wallets).save(wallet.capture());
		assertThat(wallet.getValue().getPortfolioId()).isEqualTo(TestMoney.PORTFOLIO_ID);
		assertThat(wallet.getValue().getUserId()).isEqualTo(TestUsers.USER_ID);
		assertThat(wallet.getValue().getCurrency()).isEqualTo(TestFx.USD);
		assertThat(created.name()).isEqualTo(TestMoney.PORTFOLIO_NAME);
		assertThat(created.currency()).isEqualTo(TestFx.USD);
		assertThat(created.performance().resultPercent()).isNull();
	}

	@Test
	void refusesASecondPortfolioWithTheSameName() {
		when(portfolios.existsByUserIdAndNameIgnoreCase(TestUsers.USER_ID, TestMoney.PORTFOLIO_NAME)).thenReturn(true);

		assertFailsWith(() -> service.create(TestUsers.USER_ID, NAME_WITH_SPACES, TestFx.USD), PortfolioError.NAME_TAKEN);
		verify(portfolios, never()).saveAndFlush(any(Portfolio.class));
	}

	@Test
	void refusesACurrencyThatIsNotSupported() {
		when(currencies.supported(UNKNOWN_CURRENCY)).thenThrow(new AppException(FxError.UNKNOWN_CURRENCY, UNKNOWN_CURRENCY));

		assertFailsWith(() -> service.create(TestUsers.USER_ID, TestMoney.PORTFOLIO_NAME, UNKNOWN_CURRENCY), FxError.UNKNOWN_CURRENCY);
		verify(portfolios, never()).saveAndFlush(any(Portfolio.class));
	}

	@Test
	void valuesAPortfolioInZlotyAgainstTheZlotyThatWentIntoIt() {
		when(portfolios.ownedBy(TestUsers.USER_ID, TestMoney.PORTFOLIO_ID)).thenReturn(TestMoney.portfolio());
		TransferTotals deposit = new TransferTotals(TestMoney.PORTFOLIO_ID, TransferDirection.IN, TestMoney.ARRIVED_DOLLARS, TestMoney.DEPOSITED_ZLOTY);
		givenPortfolioWallet(TestMoney.ARRIVED_DOLLARS, List.of(deposit));

		PortfolioView portfolio = service.portfolio(TestUsers.USER_ID, TestMoney.PORTFOLIO_ID);

		assertThat(portfolio.cashBalance()).isEqualByComparingTo(TestMoney.ARRIVED_DOLLARS);
		assertThat(portfolio.contributed()).isEqualByComparingTo(TestMoney.ARRIVED_DOLLARS);
		assertThat(portfolio.withdrawn()).isEqualByComparingTo(BigDecimal.ZERO);
		assertThat(portfolio.performance().currency()).isEqualTo(TestFx.PLN);
		assertThat(portfolio.performance().value()).isEqualByComparingTo(TestMoney.DOLLARS_IN_ZLOTY);
		assertThat(portfolio.performance().putIn()).isEqualByComparingTo(TestMoney.DEPOSITED_ZLOTY);
		assertThat(portfolio.performance().result()).isEqualByComparingTo("-10.64");
		assertThat(portfolio.performance().resultPercent()).isEqualByComparingTo("-2.66");
	}

	@Test
	void leavesTheValueOutWhenThereIsNoRateYet() {
		when(portfolios.findByUserIdOrderByIdAsc(TestUsers.USER_ID)).thenReturn(List.of(TestMoney.portfolio()));
		when(fx.findRate(TestFx.USD, TestFx.PLN, TestFx.THURSDAY)).thenReturn(Optional.empty());
		givenPortfolioWallet(TestMoney.ARRIVED_DOLLARS, List.of());

		PortfolioView portfolio = service.portfolios(TestUsers.USER_ID).getFirst();

		assertThat(portfolio.cashBalance()).isEqualByComparingTo(TestMoney.ARRIVED_DOLLARS);
		assertThat(portfolio.performance().value()).isNull();
		assertThat(portfolio.performance().result()).isNull();
	}

	@Test
	void aPersonWithoutPortfoliosGetsAnEmptyList() {
		assertThat(service.portfolios(TestUsers.USER_ID)).isEmpty();
	}

	private void givenPortfolioWallet(BigDecimal cashBalance, List<TransferTotals> totals) {
		when(wallets.findByPortfolioIdIn(PORTFOLIO_IDS)).thenReturn(List.of(TestMoney.portfolioWallet(TestFx.USD, cashBalance)));
		when(transfers.totalsOf(PORTFOLIO_IDS)).thenReturn(totals);
	}

	private static void assertFailsWith(ThrowingCallable call, ErrorCode expected) {
		assertThatThrownBy(call).isInstanceOf(AppException.class).extracting(thrown -> ((AppException) thrown).errorCode()).isEqualTo(expected);
	}
}
