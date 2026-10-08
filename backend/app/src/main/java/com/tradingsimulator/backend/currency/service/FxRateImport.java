package com.tradingsimulator.backend.currency.service;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.tradingsimulator.backend.currency.provider.PublishedFxRate;

public record FxRateImport(String baseCurrency, String quoteCurrency, LocalDate rateDate, BigDecimal rate, FxRateImportOutcome outcome) {

	static FxRateImport of(PublishedFxRate published, FxRateImportOutcome outcome) {
		return new FxRateImport(published.baseCurrency(), published.quoteCurrency(), published.rateDate(), published.rate(), outcome);
	}
}
