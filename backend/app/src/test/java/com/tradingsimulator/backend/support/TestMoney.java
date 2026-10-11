package com.tradingsimulator.backend.support;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalTime;

import com.tradingsimulator.backend.currency.FeeProperties;
import com.tradingsimulator.backend.portfolio.Performance;
import com.tradingsimulator.backend.portfolio.Portfolio;
import com.tradingsimulator.backend.portfolio.service.PortfolioView;
import com.tradingsimulator.backend.wallet.AccountProperties;
import com.tradingsimulator.backend.wallet.Wallet;

public final class TestMoney {

	public static final long MAIN_ACCOUNT_ID = 70L;
	public static final long PORTFOLIO_WALLET_ID = 71L;
	public static final long PORTFOLIO_ID = 700L;
	public static final long TRANSFER_ID = 7000L;
	public static final String PORTFOLIO_NAME = "Troodon's dollars";
	public static final BigDecimal STARTING_BALANCE = new BigDecimal("1000.00");
	public static final BigDecimal DEPOSITED_ZLOTY = new BigDecimal("400.00");
	public static final BigDecimal ARRIVED_DOLLARS = new BigDecimal("99.50");
	public static final BigDecimal WITHDRAWN_DOLLARS = new BigDecimal("50.00");
	public static final BigDecimal DOLLARS_PER_ZLOTY = new BigDecimal("0.25");
	public static final BigDecimal YEN_PER_ZLOTY = new BigDecimal("40.4385");
	public static final BigDecimal DOLLARS_IN_ZLOTY = new BigDecimal("389.36");
	public static final BigDecimal FX_CONVERSION_RATE = new BigDecimal("0.005");
	public static final Instant NOW = TestFx.THURSDAY.atTime(LocalTime.NOON).atZone(TestFx.WARSAW).toInstant();
	public static final Clock CLOCK = Clock.fixed(NOW, TestFx.WARSAW);

	public static AccountProperties accountProperties() {
		return new AccountProperties(TestFx.PLN, STARTING_BALANCE);
	}

	public static FeeProperties fees() {
		return new FeeProperties(FX_CONVERSION_RATE);
	}

	public static Wallet mainAccount(BigDecimal cashBalance) {
		Wallet wallet = Wallet.mainAccount(TestUsers.USER_ID, TestFx.PLN);
		wallet.setCashBalance(cashBalance);
		return TestEntities.withId(wallet, MAIN_ACCOUNT_ID);
	}

	public static Wallet portfolioWallet(String currency, BigDecimal cashBalance) {
		Wallet wallet = Wallet.ofPortfolio(TestUsers.USER_ID, PORTFOLIO_ID, currency);
		wallet.setCashBalance(cashBalance);
		return TestEntities.withId(wallet, PORTFOLIO_WALLET_ID);
	}

	public static Portfolio portfolio() {
		return TestEntities.withId(Portfolio.named(TestUsers.USER_ID, PORTFOLIO_NAME), PORTFOLIO_ID);
	}

	public static PortfolioView dollarPortfolio(Performance performance) {
		return new PortfolioView(PORTFOLIO_ID, PORTFOLIO_NAME, TestFx.USD, ARRIVED_DOLLARS, ARRIVED_DOLLARS, ARRIVED_DOLLARS, BigDecimal.ZERO, performance,
				NOW);
	}

	private TestMoney() {
	}
}
