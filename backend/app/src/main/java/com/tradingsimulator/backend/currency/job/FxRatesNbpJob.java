package com.tradingsimulator.backend.currency.job;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.tradingsimulator.backend.batch.BatchTypeCode;
import com.tradingsimulator.backend.batch.engine.BatchJobHandler;
import com.tradingsimulator.backend.batch.engine.BatchRun;
import com.tradingsimulator.backend.common.error.AppException;
import com.tradingsimulator.backend.currency.Currency;
import com.tradingsimulator.backend.currency.CurrencyRepository;
import com.tradingsimulator.backend.currency.FxError;
import com.tradingsimulator.backend.currency.FxProperties;
import com.tradingsimulator.backend.currency.FxRate;
import com.tradingsimulator.backend.currency.FxRateRepository;
import com.tradingsimulator.backend.currency.FxRateSource;
import com.tradingsimulator.backend.currency.provider.FxRateProvider;
import com.tradingsimulator.backend.currency.provider.PublishedFxRate;
import com.tradingsimulator.backend.currency.service.FxRateImportService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class FxRatesNbpJob implements BatchJobHandler {

	private static final String KEY_SEPARATOR = " ";

	private final FxRateProvider provider;
	private final FxRateRepository fxRates;
	private final CurrencyRepository currencies;
	private final FxRateImportService importService;
	private final FxProperties properties;
	private final Clock clock;

	@Override
	public BatchTypeCode type() {
		return BatchTypeCode.FX_RATES_NBP;
	}

	@Override
	public void run(BatchRun run) {
		LocalDate today = LocalDate.now(clock.withZone(properties.zone()));
		LocalDate from = fxRates.findLatestRateDate().orElseGet(() -> today.minus(properties.initialHistory()));
		Map<LocalDate, Map<String, PublishedFxRate>> tables = byDayAndCurrency(provider.ratesBetween(from, today));
		List<String> expected = foreignCurrencies();
		run.totalItems(tables.size() * expected.size());
		tables.forEach((day, rates) -> expected.forEach(code -> importOne(run, code, day, rates.get(code))));
	}

	private void importOne(BatchRun run, String code, LocalDate day, PublishedFxRate published) {
		String key = code + KEY_SEPARATOR + day;
		if (published == null) {
			run.item(key, () -> {
				throw new AppException(FxError.RATE_MISSING_FROM_SOURCE, code, FxRateSource.NBP, day);
			});
			return;
		}
		run.item(key, () -> importService.store(published));
	}

	private List<String> foreignCurrencies() {
		return currencies.findByActiveTrueOrderByCode().stream().map(Currency::getCode).filter(code -> !FxRate.PIVOT_CURRENCY.equals(code)).toList();
	}

	private static Map<LocalDate, Map<String, PublishedFxRate>> byDayAndCurrency(List<PublishedFxRate> rates) {
		Collector<PublishedFxRate, ?, Map<String, PublishedFxRate>> byCurrency = Collectors.toMap(PublishedFxRate::baseCurrency, Function.identity());
		return rates.stream().collect(Collectors.groupingBy(PublishedFxRate::rateDate, TreeMap::new, byCurrency));
	}
}
