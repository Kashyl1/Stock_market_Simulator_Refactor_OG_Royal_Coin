package com.tradingsimulator.backend.wallet;

import java.math.BigDecimal;
import java.time.Instant;

import com.tradingsimulator.backend.common.error.AppException;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "wallet")
public class Wallet extends WalletJpa {

	public static Wallet mainAccount(Long userId, String currency) {
		return opened(userId, null, currency);
	}

	public static Wallet ofPortfolio(Long userId, Long portfolioId, String currency) {
		return opened(userId, portfolioId, currency);
	}

	public boolean isMainAccount() {
		return getPortfolioId() == null;
	}

	public BigDecimal available() {
		return getCashBalance().subtract(getReservedBalance());
	}

	public WalletEntry credit(WalletEntryType type, BigDecimal amount, WalletEntryReference reference, Instant effectiveAt) {
		setCashBalance(getCashBalance().add(amount));
		return WalletEntry.recorded(this, type, amount, reference, effectiveAt);
	}

	public WalletEntry debit(WalletEntryType type, BigDecimal amount, WalletEntryReference reference, Instant effectiveAt) {
		if (available().compareTo(amount) < 0) {
			throw new AppException(WalletError.INSUFFICIENT_FUNDS, available(), getCurrency(), amount);
		}
		setCashBalance(getCashBalance().subtract(amount));
		return WalletEntry.recorded(this, type, amount.negate(), reference, effectiveAt);
	}

	private static Wallet opened(Long userId, Long portfolioId, String currency) {
		Wallet wallet = new Wallet();
		wallet.setUserId(userId);
		wallet.setPortfolioId(portfolioId);
		wallet.setCurrency(currency);
		wallet.setCashBalance(BigDecimal.ZERO);
		wallet.setReservedBalance(BigDecimal.ZERO);
		return wallet;
	}
}
