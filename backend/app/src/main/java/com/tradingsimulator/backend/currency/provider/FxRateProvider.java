package com.tradingsimulator.backend.currency.provider;

import java.time.LocalDate;
import java.util.List;

public interface FxRateProvider {

	List<PublishedFxRate> ratesBetween(LocalDate from, LocalDate to);
}
