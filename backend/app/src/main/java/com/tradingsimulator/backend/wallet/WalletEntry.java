package com.tradingsimulator.backend.wallet;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "wallet_entry")
public class WalletEntry extends WalletEntryJpa {

	static WalletEntry recorded(Wallet wallet, WalletEntryType type, BigDecimal amount, WalletEntryReference reference, Instant effectiveAt) {
		WalletEntry entry = new WalletEntry();
		entry.setWalletId(wallet.getId());
		entry.setEntryType(type);
		entry.setAmount(amount);
		entry.setBalanceAfter(wallet.getCashBalance());
		entry.setReferenceType(reference.type());
		entry.setReferenceId(reference.id());
		entry.setEffectiveAt(effectiveAt);
		return entry;
	}
}
