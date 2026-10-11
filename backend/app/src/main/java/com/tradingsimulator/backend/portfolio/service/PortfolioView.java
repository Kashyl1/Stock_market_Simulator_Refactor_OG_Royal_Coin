package com.tradingsimulator.backend.portfolio.service;

import java.math.BigDecimal;
import java.time.Instant;

import com.tradingsimulator.backend.portfolio.Performance;
import com.tradingsimulator.backend.portfolio.Portfolio;
import com.tradingsimulator.backend.portfolio.TransferSums;
import com.tradingsimulator.backend.wallet.Wallet;

public record PortfolioView(Long id, String name, String currency, BigDecimal cashBalance, BigDecimal available, BigDecimal contributed, BigDecimal withdrawn,
		Performance performance, Instant createdAt) {

	static PortfolioView of(Portfolio portfolio, Wallet wallet, TransferSums sums, Performance performance) {
		return new PortfolioView(portfolio.getId(), portfolio.getName(), wallet.getCurrency(), wallet.getCashBalance(), wallet.available(), sums.contributed(),
				sums.withdrawn(), performance, portfolio.getCreatedWhen());
	}
}
