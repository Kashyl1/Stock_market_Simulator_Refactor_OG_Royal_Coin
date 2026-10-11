package com.tradingsimulator.backend.portfolio;

import java.math.BigDecimal;

import com.tradingsimulator.backend.currency.Currency;
import com.tradingsimulator.backend.currency.FxRate;

public record TransferQuote(TransferDirection direction, BigDecimal amount, BigDecimal mainAmount, BigDecimal fxRate, BigDecimal fxFeeAmount) {

	public static TransferQuote of(TransferDirection direction, BigDecimal entered, ConversionTerms terms) {
		return switch (direction) {
			case IN -> into(entered, terms);
			case OUT -> outOf(entered, terms);
		};
	}

	public boolean arrivesAsNothing() {
		return amount.signum() <= 0 || mainAmount.signum() <= 0;
	}

	public BigDecimal leaving() {
		return direction == TransferDirection.IN ? mainAmount : amount;
	}

	public BigDecimal arriving() {
		return direction == TransferDirection.IN ? amount : mainAmount;
	}

	private static TransferQuote into(BigDecimal mainAmount, ConversionTerms terms) {
		if (!terms.converts()) {
			return new TransferQuote(TransferDirection.IN, mainAmount, mainAmount, null, BigDecimal.ZERO);
		}
		BigDecimal fee = fee(mainAmount, terms);
		BigDecimal amount = terms.portfolio().round(mainAmount.subtract(fee).multiply(terms.rate()));
		return new TransferQuote(TransferDirection.IN, amount, mainAmount, terms.rate(), fee);
	}

	private static TransferQuote outOf(BigDecimal amount, ConversionTerms terms) {
		if (!terms.converts()) {
			return new TransferQuote(TransferDirection.OUT, amount, amount, null, BigDecimal.ZERO);
		}
		BigDecimal gross = terms.main().round(amount.divide(terms.rate(), FxRate.RATE_SCALE, Currency.ROUNDING_MODE));
		BigDecimal fee = fee(gross, terms);
		return new TransferQuote(TransferDirection.OUT, amount, gross.subtract(fee), terms.rate(), fee);
	}

	private static BigDecimal fee(BigDecimal mainAmount, ConversionTerms terms) {
		return terms.main().round(mainAmount.multiply(terms.feeRate()));
	}
}
