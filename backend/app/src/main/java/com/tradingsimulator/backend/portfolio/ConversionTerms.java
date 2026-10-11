package com.tradingsimulator.backend.portfolio;

import java.math.BigDecimal;

import com.tradingsimulator.backend.currency.Currency;

public record ConversionTerms(Currency main, Currency portfolio, BigDecimal rate, BigDecimal feeRate) {

	public static ConversionTerms sameCurrency(Currency currency) {
		return new ConversionTerms(currency, currency, null, BigDecimal.ZERO);
	}

	public boolean converts() {
		return rate != null;
	}
}
