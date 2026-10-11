package com.tradingsimulator.backend.account.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.tradingsimulator.backend.portfolio.Performance;
import com.tradingsimulator.backend.portfolio.PortfolioRepository;
import com.tradingsimulator.backend.portfolio.PortfolioTransfer;
import com.tradingsimulator.backend.portfolio.PortfolioTransferRepository;
import com.tradingsimulator.backend.portfolio.TransferDirection;
import com.tradingsimulator.backend.portfolio.TransferQuote;
import com.tradingsimulator.backend.portfolio.service.PortfolioService;
import com.tradingsimulator.backend.portfolio.service.PortfolioView;
import com.tradingsimulator.backend.support.TestEntities;
import com.tradingsimulator.backend.support.TestFx;
import com.tradingsimulator.backend.support.TestMoney;
import com.tradingsimulator.backend.support.TestUsers;
import com.tradingsimulator.backend.wallet.Wallet;
import com.tradingsimulator.backend.wallet.WalletEntry;
import com.tradingsimulator.backend.wallet.WalletEntryReference;
import com.tradingsimulator.backend.wallet.WalletEntryRepository;
import com.tradingsimulator.backend.wallet.WalletEntryType;
import com.tradingsimulator.backend.wallet.WalletRepository;
import com.tradingsimulator.backend.web.PageResponse;

class AccountServiceImplTest {

	private static final BigDecimal MAIN_CASH = new BigDecimal("600.00");
	private static final int FIRST_PAGE = 0;
	private static final int PAGE_SIZE = 20;
	private static final long DEPOSIT_ENTRY_ID = 1L;
	private static final long TRANSFER_ENTRY_ID = 2L;

	private final WalletRepository wallets = mock(WalletRepository.class);
	private final WalletEntryRepository entries = mock(WalletEntryRepository.class);
	private final PortfolioRepository portfolios = mock(PortfolioRepository.class);
	private final PortfolioTransferRepository transfers = mock(PortfolioTransferRepository.class);
	private final PortfolioService portfolioService = mock(PortfolioService.class);

	private final AccountService service = new AccountServiceImpl(wallets, entries, portfolios, transfers, portfolioService);

	private final Wallet mainAccount = TestMoney.mainAccount(MAIN_CASH);

	@BeforeEach
	void stubMainAccount() {
		when(wallets.mainAccountOf(TestUsers.USER_ID)).thenReturn(mainAccount);
		when(entries.sumOfType(TestMoney.MAIN_ACCOUNT_ID, WalletEntryType.DEPOSIT)).thenReturn(TestMoney.STARTING_BALANCE);
	}

	@Test
	void theAccountIsWorthItsCashPlusEveryPortfolioInZloty() {
		when(portfolioService.portfolios(TestUsers.USER_ID)).thenReturn(List.of(portfolioWorth(TestMoney.DOLLARS_IN_ZLOTY)));

		AccountView account = service.account(TestUsers.USER_ID);

		assertThat(account.cashBalance()).isEqualByComparingTo(MAIN_CASH);
		assertThat(account.performance().value()).isEqualByComparingTo("989.36");
		assertThat(account.performance().putIn()).isEqualByComparingTo(TestMoney.STARTING_BALANCE);
		assertThat(account.performance().result()).isEqualByComparingTo("-10.64");
		assertThat(account.performance().resultPercent()).isEqualByComparingTo("-1.06");
	}

	@Test
	void aPortfolioWithoutAValueLeavesTheAccountValueOut() {
		when(portfolioService.portfolios(TestUsers.USER_ID)).thenReturn(List.of(portfolioWorth(null)));

		AccountView account = service.account(TestUsers.USER_ID);

		assertThat(account.cashBalance()).isEqualByComparingTo(MAIN_CASH);
		assertThat(account.performance().value()).isNull();
	}

	@Test
	void theHistoryNamesThePortfolioTheMoneyWentTo() {
		Wallet ledger = TestMoney.mainAccount(BigDecimal.ZERO);
		WalletEntry deposit = TestEntities.withId(ledger.credit(WalletEntryType.DEPOSIT, TestMoney.STARTING_BALANCE, WalletEntryReference.startingBalance(),
				TestMoney.NOW), DEPOSIT_ENTRY_ID);
		WalletEntry transferOut = TestEntities.withId(ledger.debit(WalletEntryType.TRANSFER_OUT, TestMoney.DEPOSITED_ZLOTY,
				WalletEntryReference.portfolioTransfer(TestMoney.TRANSFER_ID), TestMoney.NOW), TRANSFER_ENTRY_ID);
		when(entries.findByWalletId(eq(TestMoney.MAIN_ACCOUNT_ID), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(transferOut, deposit)));
		when(transfers.findAllById(Set.of(TestMoney.TRANSFER_ID))).thenReturn(List.of(transfer()));
		when(portfolios.findAllById(Set.of(TestMoney.PORTFOLIO_ID))).thenReturn(List.of(TestMoney.portfolio()));

		PageResponse<AccountEntryView> history = service.entries(TestUsers.USER_ID, FIRST_PAGE, PAGE_SIZE);

		assertThat(history.content()).extracting(AccountEntryView::type).containsExactly(WalletEntryType.TRANSFER_OUT, WalletEntryType.DEPOSIT);
		assertThat(history.content()).extracting(AccountEntryView::portfolioName).containsExactly(TestMoney.PORTFOLIO_NAME, null);
		assertThat(history.content()).extracting(AccountEntryView::currency).containsOnly(TestFx.PLN);
	}

	private static PortfolioView portfolioWorth(BigDecimal value) {
		return TestMoney.dollarPortfolio(Performance.of(TestFx.PLN, value, TestMoney.DEPOSITED_ZLOTY, BigDecimal.ZERO));
	}

	private static PortfolioTransfer transfer() {
		TransferQuote quote = new TransferQuote(TransferDirection.IN, TestMoney.ARRIVED_DOLLARS, TestMoney.DEPOSITED_ZLOTY, null, BigDecimal.ZERO);
		return TestEntities.withId(PortfolioTransfer.of(TestMoney.PORTFOLIO_ID, quote, TestMoney.NOW), TestMoney.TRANSFER_ID);
	}
}
