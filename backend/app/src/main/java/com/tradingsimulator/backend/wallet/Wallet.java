package com.tradingsimulator.backend.wallet;

import java.math.BigDecimal;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "wallet")
public class Wallet extends WalletJpa {

	public static Wallet empty(Long userId) {
		Wallet wallet = new Wallet();
		wallet.setUserId(userId);
		wallet.setCurrency(DEFAULT_CURRENCY);
		wallet.setCashBalance(BigDecimal.ZERO);
		wallet.setReservedBalance(BigDecimal.ZERO);
		return wallet;
	}
}
