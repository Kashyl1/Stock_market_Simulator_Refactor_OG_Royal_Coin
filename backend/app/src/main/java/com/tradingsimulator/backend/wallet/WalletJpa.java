package com.tradingsimulator.backend.wallet;

import java.math.BigDecimal;

import com.tradingsimulator.backend.common.persistence.AbstractEntity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

@MappedSuperclass
@Getter
@Setter
public class WalletJpa extends AbstractEntity {

	public static final String DEFAULT_CURRENCY = "USD";

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(nullable = false, length = 3)
	private String currency;

	@Column(name = "cash_balance", nullable = false, precision = 20, scale = 2)
	private BigDecimal cashBalance;

	@Column(name = "reserved_balance", nullable = false, precision = 20, scale = 2)
	private BigDecimal reservedBalance;
}
