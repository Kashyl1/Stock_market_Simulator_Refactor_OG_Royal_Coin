package com.tradingsimulator.backend.currency;

import java.math.BigDecimal;
import java.time.LocalDate;

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
public class FxRateJpa extends AbstractEntity {

	private static final int RATE_PRECISION = 38;
	private static final int RATE_SCALE = 18;
	private static final int SOURCE_MAX_LENGTH = 30;

	@Column(name = "base_currency", nullable = false, length = CurrencyJpa.CODE_LENGTH)
	private String baseCurrency;

	@Column(name = "quote_currency", nullable = false, length = CurrencyJpa.CODE_LENGTH)
	private String quoteCurrency;

	@Column(name = "rate_date", nullable = false)
	private LocalDate rateDate;

	@Column(nullable = false, precision = RATE_PRECISION, scale = RATE_SCALE)
	private BigDecimal rate;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = SOURCE_MAX_LENGTH)
	private FxRateSource source;
}
