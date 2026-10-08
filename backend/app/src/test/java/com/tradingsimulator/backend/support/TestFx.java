package com.tradingsimulator.backend.support;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;

import com.tradingsimulator.backend.currency.Currency;
import com.tradingsimulator.backend.currency.FxProperties;
import com.tradingsimulator.backend.currency.FxRate;
import com.tradingsimulator.backend.currency.FxRateSource;
import com.tradingsimulator.backend.currency.provider.PublishedFxRate;

public final class TestFx {

	public static final String PLN = FxRate.PIVOT_CURRENCY;
	public static final String USD = "USD";
	public static final String EUR = "EUR";
	public static final String JPY = "JPY";
	public static final String CLP = "CLP";
	public static final int CENTS = 2;
	public static final int WHOLE_UNITS = 0;
	public static final LocalDate WEDNESDAY = LocalDate.of(2026, 10, 7);
	public static final LocalDate THURSDAY = LocalDate.of(2026, 10, 8);
	public static final BigDecimal USD_MID = new BigDecimal("3.9132");
	public static final BigDecimal EUR_MID = new BigDecimal("4.3789");
	public static final BigDecimal JPY_MID = new BigDecimal("0.024729");
	public static final String NBP_BASE_URL = "https://api.nbp.test/api";
	public static final ZoneId WARSAW = ZoneId.of("Europe/Warsaw");
	public static final Period INITIAL_HISTORY = Period.ofDays(365);
	public static final int DAYS_PER_REQUEST = 93;

	private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
	private static final Duration READ_TIMEOUT = Duration.ofSeconds(10);

	public static FxProperties properties() {
		return new FxProperties(WARSAW, INITIAL_HISTORY, new FxProperties.Nbp(NBP_BASE_URL, DAYS_PER_REQUEST, CONNECT_TIMEOUT, READ_TIMEOUT));
	}

	public static Currency currency(String code, int minorUnits) {
		return Currency.active(code, code, minorUnits);
	}

	public static PublishedFxRate published(String code, LocalDate day, BigDecimal mid) {
		return new PublishedFxRate(code, PLN, day, mid, FxRateSource.NBP);
	}

	public static FxRate stored(String code, LocalDate day, BigDecimal mid) {
		return FxRate.published(published(code, day, mid));
	}

	private TestFx() {
	}
}
