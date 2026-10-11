package com.tradingsimulator.backend.account.service;

import java.math.BigDecimal;
import java.time.Instant;

import com.tradingsimulator.backend.portfolio.Portfolio;
import com.tradingsimulator.backend.wallet.WalletEntry;
import com.tradingsimulator.backend.wallet.WalletEntryType;

public record AccountEntryView(Long id, WalletEntryType type, BigDecimal amount, String currency, BigDecimal balanceAfter, Instant effectiveAt,
		Long portfolioId, String portfolioName) {

	static AccountEntryView of(WalletEntry entry, String currency, Portfolio portfolio) {
		Long portfolioId = portfolio == null ? null : portfolio.getId();
		String portfolioName = portfolio == null ? null : portfolio.getName();
		return new AccountEntryView(entry.getId(), entry.getEntryType(), entry.getAmount(), currency, entry.getBalanceAfter(), entry.getEffectiveAt(),
				portfolioId, portfolioName);
	}
}
