package com.tradingsimulator.backend.wallet;

import java.math.BigDecimal;
import java.time.Instant;

import com.tradingsimulator.backend.common.persistence.AbstractEntity;

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

@MappedSuperclass
@Getter
@Setter
public class WalletEntryJpa extends AbstractEntity {

	@Column(name = "wallet_id", nullable = false)
	private Long walletId;

	@Enumerated(EnumType.STRING)
	@Column(name = "entry_type", nullable = false, length = 30)
	private WalletEntryType entryType;

	@Column(nullable = false, precision = 20, scale = 2)
	private BigDecimal amount;

	@Column(name = "balance_after", nullable = false, precision = 20, scale = 2)
	private BigDecimal balanceAfter;

	@Enumerated(EnumType.STRING)
	@Column(name = "reference_type", length = 40)
	private WalletEntryReferenceType referenceType;

	@Column(name = "reference_id")
	private Long referenceId;

	@Column(name = "effective_at", nullable = false)
	private Instant effectiveAt;
}
