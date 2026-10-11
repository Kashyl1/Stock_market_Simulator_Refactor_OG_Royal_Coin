package com.tradingsimulator.backend.portfolio;

import java.math.BigDecimal;
import java.time.Instant;

import com.tradingsimulator.backend.common.persistence.AbstractEntity;
import com.tradingsimulator.backend.currency.FxRate;

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

@MappedSuperclass
@Getter
@Setter
public class PortfolioTransferJpa extends AbstractEntity {

	@Column(name = "portfolio_id", nullable = false)
	private Long portfolioId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private TransferDirection direction;

	@Column(nullable = false, precision = 20, scale = 2)
	private BigDecimal amount;

	@Column(name = "main_amount", nullable = false, precision = 20, scale = 2)
	private BigDecimal mainAmount;

	@Column(name = "fx_rate", precision = FxRate.RATE_PRECISION, scale = FxRate.RATE_SCALE)
	private BigDecimal fxRate;

	@Column(name = "fx_fee_amount", nullable = false, precision = 20, scale = 2)
	private BigDecimal fxFeeAmount;

	@Column(name = "transferred_at", nullable = false)
	private Instant transferredAt;
}
