package com.tradingsimulator.backend.currency.provider;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.tradingsimulator.backend.currency.FxRateSource;

public record PublishedFxRate(String baseCurrency, String quoteCurrency, LocalDate rateDate, BigDecimal rate, FxRateSource source) {
}
