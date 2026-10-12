package com.tradingsimulator.backend.portfolio;

import java.math.BigDecimal;

import com.tradingsimulator.backend.currency.Currency;

public record Performance(String currency, BigDecimal value, BigDecimal putIn, BigDecimal takenOut, BigDecimal netDeposits, BigDecimal result,
		BigDecimal resultPercent) {

	private static final BigDecimal PERCENT = BigDecimal.valueOf(100);
	private static final int PERCENT_SCALE = 2;

	public static Performance of(String currency, BigDecimal value, BigDecimal putIn, BigDecimal takenOut) {
		BigDecimal netDeposits = putIn.subtract(takenOut);
		if (value == null) {
			return new Performance(currency, null, putIn, takenOut, netDeposits, null, null);
		}
		BigDecimal result = value.subtract(netDeposits);
		return new Performance(currency, value, putIn, takenOut, netDeposits, result, percentOf(result, putIn));
	}

	private static BigDecimal percentOf(BigDecimal result, BigDecimal putIn) {
		if (putIn.signum() == 0) {
			return null;
		}
		return result.multiply(PERCENT).divide(putIn, PERCENT_SCALE, Currency.ROUNDING_MODE);
	}
}
