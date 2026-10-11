package com.tradingsimulator.backend.portfolio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.tradingsimulator.backend.account.service.AccountService;
import com.tradingsimulator.backend.account.service.AccountView;
import com.tradingsimulator.backend.auth.Role;
import com.tradingsimulator.backend.auth.User;
import com.tradingsimulator.backend.auth.UserRepository;
import com.tradingsimulator.backend.common.error.AppException;
import com.tradingsimulator.backend.currency.FxRateRepository;
import com.tradingsimulator.backend.portfolio.service.PortfolioService;
import com.tradingsimulator.backend.portfolio.service.PortfolioTransferService;
import com.tradingsimulator.backend.portfolio.service.PortfolioView;
import com.tradingsimulator.backend.support.TestDatabase;
import com.tradingsimulator.backend.support.TestFx;
import com.tradingsimulator.backend.support.TestJwtKeys;
import com.tradingsimulator.backend.support.TestMoney;
import com.tradingsimulator.backend.support.TestTags;
import com.tradingsimulator.backend.support.TestUsers;
import com.tradingsimulator.backend.wallet.Wallet;
import com.tradingsimulator.backend.wallet.WalletEntry;
import com.tradingsimulator.backend.wallet.WalletEntryRepository;
import com.tradingsimulator.backend.wallet.WalletEntryType;
import com.tradingsimulator.backend.wallet.WalletRepository;
import com.tradingsimulator.backend.wallet.service.MainAccountService;

@SpringBootTest(properties = { TestDatabase.FLYWAY_ENABLED, TestDatabase.HIBERNATE_VALIDATE, TestDatabase.BATCH_ENGINE_OFF })
@Tag(TestTags.INTEGRATION)
@Testcontainers
class PortfolioCashIntegrationTest {

	private static final String TRANSFERS_EMAIL = "transfers.troodon@example.com";
	private static final String WALLETS_EMAIL = "wallets.troodon@example.com";
	private static final String OWNER_EMAIL = "owner.troodon@example.com";
	private static final String STRANGER_EMAIL = "stranger.troodon@example.com";
	private static final String BALANCE_EMAIL = "balance.troodon@example.com";
	private static final String RATE_EMAIL = "rate.troodon@example.com";
	private static final String NAMES_EMAIL = "names.troodon@example.com";
	private static final String PORTFOLIO_NAME_IN_CAPITALS = TestMoney.PORTFOLIO_NAME.toUpperCase(Locale.ROOT);
	private static final String SECOND_PORTFOLIO_NAME = "Troodon's second portfolio";
	private static final BigDecimal ONE_ZLOTY = BigDecimal.ONE;
	private static final List<WalletEntryType> MAIN_ACCOUNT_HISTORY = List.of(WalletEntryType.DEPOSIT, WalletEntryType.TRANSFER_OUT,
			WalletEntryType.TRANSFER_IN);

	@Container
	@ServiceConnection
	static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(TestDatabase.POSTGRES_IMAGE);

	@Autowired
	private UserRepository users;

	@Autowired
	private WalletRepository wallets;

	@Autowired
	private WalletEntryRepository entries;

	@Autowired
	private PortfolioRepository portfolios;

	@Autowired
	private PortfolioTransferRepository transfers;

	@Autowired
	private FxRateRepository fxRates;

	@Autowired
	private MainAccountService mainAccounts;

	@Autowired
	private PortfolioService portfolioService;

	@Autowired
	private PortfolioTransferService transferService;

	@Autowired
	private AccountService accountService;

	@Autowired
	private Clock clock;

	@DynamicPropertySource
	static void jwtKeys(DynamicPropertyRegistry registry) {
		TestJwtKeys.register(registry);
	}

	@Test
	void aVerifiedPersonMovesZlotyIntoADollarPortfolioAndBack() {
		Long userId = fundedTroodon(TRANSFERS_EMAIL);
		fxRates.save(TestFx.stored(TestFx.USD, LocalDate.now(clock.withZone(TestFx.WARSAW)), TestFx.USD_MID));
		PortfolioView dollars = portfolioService.create(userId, TestMoney.PORTFOLIO_NAME, TestFx.USD);

		transferService.transfer(userId, dollars.id(), TransferDirection.IN, TestMoney.DEPOSITED_ZLOTY);
		transferService.transfer(userId, dollars.id(), TransferDirection.OUT, TestMoney.WITHDRAWN_DOLLARS);

		PortfolioView portfolio = portfolioService.portfolio(userId, dollars.id());
		assertThat(portfolio.cashBalance()).isEqualByComparingTo(portfolio.contributed().subtract(TestMoney.WITHDRAWN_DOLLARS));
		assertThat(portfolio.withdrawn()).isEqualByComparingTo(TestMoney.WITHDRAWN_DOLLARS);
		assertThat(portfolio.performance().putIn()).isEqualByComparingTo(TestMoney.DEPOSITED_ZLOTY);
		assertThat(portfolio.performance().result()).isNegative();
		Wallet mainAccount = wallets.mainAccountOf(userId);
		BigDecimal mainCash = TestMoney.STARTING_BALANCE.subtract(TestMoney.DEPOSITED_ZLOTY).add(portfolio.performance().takenOut());
		assertThat(mainAccount.getCashBalance()).isEqualByComparingTo(mainCash);
		List<WalletEntry> history = entries.findByWalletId(mainAccount.getId(), Pageable.unpaged()).getContent();
		assertThat(history).extracting(WalletEntry::getEntryType).containsExactlyInAnyOrderElementsOf(MAIN_ACCOUNT_HISTORY);
		AccountView account = accountService.account(userId);
		assertThat(account.performance().putIn()).isEqualByComparingTo(TestMoney.STARTING_BALANCE);
		assertThat(account.performance().value()).isEqualByComparingTo(mainAccount.getCashBalance().add(portfolio.performance().value()));
	}

	@Test
	void aPersonHasOneMainAccountAndEachPortfolioOneWallet() {
		Long userId = fundedTroodon(WALLETS_EMAIL);
		Portfolio portfolio = portfolios.saveAndFlush(Portfolio.named(userId, TestMoney.PORTFOLIO_NAME));
		wallets.saveAndFlush(Wallet.ofPortfolio(userId, portfolio.getId(), TestFx.USD));

		assertThatThrownBy(() -> wallets.saveAndFlush(Wallet.mainAccount(userId, TestFx.PLN))).isInstanceOf(DataIntegrityViolationException.class);
		assertThatThrownBy(() -> wallets.saveAndFlush(Wallet.ofPortfolio(userId, portfolio.getId(), TestFx.EUR)))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void aWalletCannotBelongToSomeoneElsesPortfolio() {
		Long ownerId = fundedTroodon(OWNER_EMAIL);
		Long strangerId = fundedTroodon(STRANGER_EMAIL);
		Portfolio owned = portfolios.saveAndFlush(Portfolio.named(ownerId, TestMoney.PORTFOLIO_NAME));

		assertThatThrownBy(() -> wallets.saveAndFlush(Wallet.ofPortfolio(strangerId, owned.getId(), TestFx.USD)))
				.isInstanceOf(DataIntegrityViolationException.class);
		assertThatThrownBy(() -> portfolios.ownedBy(strangerId, owned.getId())).isInstanceOf(AppException.class)
				.extracting(thrown -> ((AppException) thrown).errorCode()).isEqualTo(PortfolioError.NOT_FOUND);
	}

	@Test
	void aBalanceCannotGoBelowZero() {
		Wallet mainAccount = wallets.mainAccountOf(fundedTroodon(BALANCE_EMAIL));
		mainAccount.setCashBalance(ONE_ZLOTY.negate());

		assertThatThrownBy(() -> wallets.saveAndFlush(mainAccount)).isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void aTransferWithoutARateMustMoveTheSameAmountOnBothSides() {
		Portfolio portfolio = portfolios.saveAndFlush(Portfolio.named(fundedTroodon(RATE_EMAIL), TestMoney.PORTFOLIO_NAME));
		TransferQuote unbalanced = new TransferQuote(TransferDirection.IN, ONE_ZLOTY, TestMoney.DEPOSITED_ZLOTY, null, BigDecimal.ZERO);

		assertThatThrownBy(() -> transfers.saveAndFlush(PortfolioTransfer.of(portfolio.getId(), unbalanced, clock.instant())))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void portfolioNamesAreUniquePerPersonRegardlessOfCase() {
		Long userId = fundedTroodon(NAMES_EMAIL);
		portfolios.saveAndFlush(Portfolio.named(userId, TestMoney.PORTFOLIO_NAME));
		portfolios.saveAndFlush(Portfolio.named(userId, SECOND_PORTFOLIO_NAME));

		assertThat(portfolios.existsByUserIdAndNameIgnoreCase(userId, PORTFOLIO_NAME_IN_CAPITALS)).isTrue();
		assertThatThrownBy(() -> portfolios.saveAndFlush(Portfolio.named(userId, PORTFOLIO_NAME_IN_CAPITALS)))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	private Long fundedTroodon(String email) {
		User troodon = users.save(User.active(email, TestUsers.PASSWORD_HASH, TestUsers.DISPLAY_NAME, Role.USER));
		mainAccounts.open(troodon.getId());
		mainAccounts.grantStartingBalance(troodon.getId());
		return troodon.getId();
	}
}
