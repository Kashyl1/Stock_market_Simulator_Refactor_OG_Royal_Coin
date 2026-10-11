package com.tradingsimulator.backend.portfolio.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.tradingsimulator.backend.common.error.AppException;
import com.tradingsimulator.backend.common.error.ErrorCode;
import com.tradingsimulator.backend.currency.CurrencyRepository;
import com.tradingsimulator.backend.currency.FxError;
import com.tradingsimulator.backend.currency.service.FxConversionService;
import com.tradingsimulator.backend.portfolio.PortfolioError;
import com.tradingsimulator.backend.portfolio.PortfolioRepository;
import com.tradingsimulator.backend.portfolio.PortfolioTransfer;
import com.tradingsimulator.backend.portfolio.PortfolioTransferRepository;
import com.tradingsimulator.backend.portfolio.TransferDirection;
import com.tradingsimulator.backend.support.TestEntities;
import com.tradingsimulator.backend.support.TestFx;
import com.tradingsimulator.backend.support.TestMoney;
import com.tradingsimulator.backend.support.TestUsers;
import com.tradingsimulator.backend.wallet.Wallet;
import com.tradingsimulator.backend.wallet.WalletEntry;
import com.tradingsimulator.backend.wallet.WalletEntryReferenceType;
import com.tradingsimulator.backend.wallet.WalletEntryRepository;
import com.tradingsimulator.backend.wallet.WalletEntryType;
import com.tradingsimulator.backend.wallet.WalletError;
import com.tradingsimulator.backend.wallet.WalletRepository;

class PortfolioTransferServiceImplTest {

	private static final BigDecimal MORE_THAN_THE_ACCOUNT_HAS = new BigDecimal("1500.00");
	private static final int TWO_SIDES = 2;

	private final PortfolioRepository portfolios = mock(PortfolioRepository.class);
	private final PortfolioTransferRepository transfers = mock(PortfolioTransferRepository.class);
	private final WalletRepository wallets = mock(WalletRepository.class);
	private final WalletEntryRepository entries = mock(WalletEntryRepository.class);
	private final CurrencyRepository currencies = mock(CurrencyRepository.class);
	private final FxConversionService fx = mock(FxConversionService.class);

	private final PortfolioTransferService service = new PortfolioTransferServiceImpl(portfolios, transfers, wallets, entries, currencies, fx, TestMoney.fees(),
			TestFx.properties(), TestMoney.CLOCK);

	@BeforeEach
	void stubPortfolio() {
		when(portfolios.ownedBy(TestUsers.USER_ID, TestMoney.PORTFOLIO_ID)).thenReturn(TestMoney.portfolio());
		when(currencies.supported(TestFx.PLN)).thenReturn(TestFx.currency(TestFx.PLN, TestFx.CENTS));
		when(currencies.supported(TestFx.USD)).thenReturn(TestFx.currency(TestFx.USD, TestFx.CENTS));
		when(currencies.supported(TestFx.JPY)).thenReturn(TestFx.currency(TestFx.JPY, TestFx.WHOLE_UNITS));
		when(fx.rate(TestFx.PLN, TestFx.USD, TestFx.THURSDAY)).thenReturn(TestMoney.DOLLARS_PER_ZLOTY);
		when(fx.rate(TestFx.PLN, TestFx.JPY, TestFx.THURSDAY)).thenReturn(TestMoney.YEN_PER_ZLOTY);
		when(transfers.save(any(PortfolioTransfer.class))).thenAnswer(invocation -> TestEntities.withId(invocation.getArgument(0), TestMoney.TRANSFER_ID));
	}

	@Test
	void aDepositConvertsZlotyIntoDollarsAndRecordsBothSides() {
		Wallet mainAccount = TestMoney.mainAccount(TestMoney.STARTING_BALANCE);
		Wallet dollars = TestMoney.portfolioWallet(TestFx.USD, BigDecimal.ZERO);
		givenLocked(mainAccount, dollars);

		TransferView transfer = service.transfer(TestUsers.USER_ID, TestMoney.PORTFOLIO_ID, TransferDirection.IN, TestMoney.DEPOSITED_ZLOTY);

		assertThat(mainAccount.getCashBalance()).isEqualByComparingTo("600.00");
		assertThat(dollars.getCashBalance()).isEqualByComparingTo(TestMoney.ARRIVED_DOLLARS);
		assertThat(transfer.id()).isEqualTo(TestMoney.TRANSFER_ID);
		assertThat(transfer.amount()).isEqualByComparingTo(TestMoney.ARRIVED_DOLLARS);
		assertThat(transfer.mainAmount()).isEqualByComparingTo(TestMoney.DEPOSITED_ZLOTY);
		assertThat(transfer.fxFeeAmount()).isEqualByComparingTo("2.00");
		assertThat(transfer.currency()).isEqualTo(TestFx.USD);
		assertThat(transfer.mainCurrency()).isEqualTo(TestFx.PLN);
		List<WalletEntry> recorded = recordedEntries();
		assertThat(recorded).extracting(WalletEntry::getWalletId).containsExactly(TestMoney.MAIN_ACCOUNT_ID, TestMoney.PORTFOLIO_WALLET_ID);
		assertThat(recorded).extracting(WalletEntry::getEntryType).containsExactly(WalletEntryType.TRANSFER_OUT, WalletEntryType.TRANSFER_IN);
		assertThat(recorded.getFirst().getAmount()).isEqualByComparingTo(TestMoney.DEPOSITED_ZLOTY.negate());
		assertThat(recorded.getLast().getAmount()).isEqualByComparingTo(TestMoney.ARRIVED_DOLLARS);
		assertThat(recorded).allSatisfy(entry -> {
			assertThat(entry.getReferenceType()).isEqualTo(WalletEntryReferenceType.PORTFOLIO_TRANSFER);
			assertThat(entry.getReferenceId()).isEqualTo(TestMoney.TRANSFER_ID);
		});
	}

	@Test
	void aWithdrawalConvertsDollarsBackIntoZloty() {
		Wallet mainAccount = TestMoney.mainAccount(BigDecimal.ZERO);
		Wallet dollars = TestMoney.portfolioWallet(TestFx.USD, TestMoney.ARRIVED_DOLLARS);
		givenLocked(mainAccount, dollars);

		service.transfer(TestUsers.USER_ID, TestMoney.PORTFOLIO_ID, TransferDirection.OUT, TestMoney.WITHDRAWN_DOLLARS);

		assertThat(dollars.getCashBalance()).isEqualByComparingTo("49.50");
		assertThat(mainAccount.getCashBalance()).isEqualByComparingTo("199.00");
		assertThat(recordedEntries()).extracting(WalletEntry::getWalletId).containsExactly(TestMoney.PORTFOLIO_WALLET_ID, TestMoney.MAIN_ACCOUNT_ID);
	}

	@Test
	void theSameCurrencyMovesWithoutAskingForARate() {
		Wallet zloty = TestMoney.portfolioWallet(TestFx.PLN, BigDecimal.ZERO);
		givenLocked(TestMoney.mainAccount(TestMoney.STARTING_BALANCE), zloty);

		TransferView transfer = service.transfer(TestUsers.USER_ID, TestMoney.PORTFOLIO_ID, TransferDirection.IN, TestMoney.DEPOSITED_ZLOTY);

		assertThat(zloty.getCashBalance()).isEqualByComparingTo(TestMoney.DEPOSITED_ZLOTY);
		assertThat(transfer.fxRate()).isNull();
		verifyNoInteractions(fx);
	}

	@Test
	void refusesMoreThanTheMainAccountHas() {
		givenLocked(TestMoney.mainAccount(TestMoney.STARTING_BALANCE), TestMoney.portfolioWallet(TestFx.USD, BigDecimal.ZERO));

		assertFailsWith(() -> service.transfer(TestUsers.USER_ID, TestMoney.PORTFOLIO_ID, TransferDirection.IN, MORE_THAN_THE_ACCOUNT_HAS),
				WalletError.INSUFFICIENT_FUNDS);
		verify(entries, never()).save(any(WalletEntry.class));
	}

	@Test
	void refusesMoreDecimalPlacesThanTheCurrencyHas() {
		givenLocked(TestMoney.mainAccount(TestMoney.STARTING_BALANCE), TestMoney.portfolioWallet(TestFx.USD, BigDecimal.ZERO));

		assertFailsWith(() -> service.transfer(TestUsers.USER_ID, TestMoney.PORTFOLIO_ID, TransferDirection.IN, new BigDecimal("10.001")),
				PortfolioError.AMOUNT_TOO_PRECISE);
	}

	@Test
	void refusesAnAmountThatWouldArriveAsNothing() {
		givenLocked(TestMoney.mainAccount(TestMoney.STARTING_BALANCE), TestMoney.portfolioWallet(TestFx.JPY, BigDecimal.ZERO));

		assertFailsWith(() -> service.transfer(TestUsers.USER_ID, TestMoney.PORTFOLIO_ID, TransferDirection.IN, new BigDecimal("0.01")),
				PortfolioError.TRANSFER_TOO_SMALL);
		verify(transfers, never()).save(any(PortfolioTransfer.class));
	}

	@Test
	void aDayWithoutARateRefusesTheTransfer() {
		givenLocked(TestMoney.mainAccount(TestMoney.STARTING_BALANCE), TestMoney.portfolioWallet(TestFx.USD, BigDecimal.ZERO));
		when(fx.rate(TestFx.PLN, TestFx.USD, TestFx.THURSDAY)).thenThrow(new AppException(FxError.RATE_NOT_AVAILABLE, TestFx.USD, TestFx.THURSDAY));

		assertFailsWith(() -> service.transfer(TestUsers.USER_ID, TestMoney.PORTFOLIO_ID, TransferDirection.IN, TestMoney.DEPOSITED_ZLOTY),
				FxError.RATE_NOT_AVAILABLE);
	}

	@Test
	void aQuoteShowsTheConversionWithoutMovingMoney() {
		Wallet mainAccount = TestMoney.mainAccount(TestMoney.STARTING_BALANCE);
		when(wallets.mainAccountOf(TestUsers.USER_ID)).thenReturn(mainAccount);
		when(wallets.ofPortfolio(TestMoney.PORTFOLIO_ID)).thenReturn(TestMoney.portfolioWallet(TestFx.USD, BigDecimal.ZERO));

		TransferQuoteView quote = service.quote(TestUsers.USER_ID, TestMoney.PORTFOLIO_ID, TransferDirection.IN, TestMoney.DEPOSITED_ZLOTY);

		assertThat(quote.amount()).isEqualByComparingTo(TestMoney.ARRIVED_DOLLARS);
		assertThat(quote.fxRate()).isEqualByComparingTo(TestMoney.DOLLARS_PER_ZLOTY);
		assertThat(mainAccount.getCashBalance()).isEqualByComparingTo(TestMoney.STARTING_BALANCE);
		verifyNoInteractions(transfers, entries);
	}

	private void givenLocked(Wallet mainAccount, Wallet portfolioWallet) {
		when(wallets.lockMainAccountAndPortfolio(TestUsers.USER_ID, TestMoney.PORTFOLIO_ID)).thenReturn(List.of(mainAccount, portfolioWallet));
	}

	private List<WalletEntry> recordedEntries() {
		ArgumentCaptor<WalletEntry> entry = ArgumentCaptor.forClass(WalletEntry.class);
		verify(entries, times(TWO_SIDES)).save(entry.capture());
		return entry.getAllValues();
	}

	private static void assertFailsWith(ThrowingCallable call, ErrorCode expected) {
		assertThatThrownBy(call).isInstanceOf(AppException.class).extracting(thrown -> ((AppException) thrown).errorCode()).isEqualTo(expected);
	}
}
