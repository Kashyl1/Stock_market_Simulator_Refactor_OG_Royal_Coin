package com.tradingsimulator.backend.currency.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.tradingsimulator.backend.batch.engine.BatchItemWork;
import com.tradingsimulator.backend.batch.engine.BatchRun;
import com.tradingsimulator.backend.common.error.AppException;
import com.tradingsimulator.backend.currency.CurrencyRepository;
import com.tradingsimulator.backend.currency.FxError;
import com.tradingsimulator.backend.currency.FxRateRepository;
import com.tradingsimulator.backend.currency.provider.FxRateProvider;
import com.tradingsimulator.backend.currency.provider.PublishedFxRate;
import com.tradingsimulator.backend.currency.service.FxRateImportService;
import com.tradingsimulator.backend.support.TestFx;

class FxRatesNbpJobTest {

	private static final Instant THURSDAY_MORNING = Instant.parse("2026-10-08T10:00:00Z");
	private static final Instant THURSDAY_LATE_EVENING_IN_UTC = Instant.parse("2026-10-08T22:30:00Z");
	private static final LocalDate FRIDAY = TestFx.THURSDAY.plusDays(1);
	private static final LocalDate MONDAY = LocalDate.of(2026, 10, 5);
	private static final String KEY_SEPARATOR = " ";

	private final FxRateProvider provider = mock(FxRateProvider.class);
	private final FxRateRepository fxRates = mock(FxRateRepository.class);
	private final CurrencyRepository currencies = mock(CurrencyRepository.class);
	private final FxRateImportService importService = mock(FxRateImportService.class);
	private final RecordingRun run = new RecordingRun();

	@BeforeEach
	void stubCurrencies() {
		when(currencies.findByActiveTrueOrderByCode()).thenReturn(List.of(TestFx.currency(TestFx.EUR, TestFx.CENTS), TestFx.currency(TestFx.PLN, TestFx.CENTS),
				TestFx.currency(TestFx.USD, TestFx.CENTS)));
	}

	@Test
	void startsAYearBackWhenNoRateIsStored() {
		jobAt(THURSDAY_MORNING).run(run);

		verify(provider).ratesBetween(TestFx.THURSDAY.minus(TestFx.INITIAL_HISTORY), TestFx.THURSDAY);
	}

	@Test
	void continuesFromTheNewestStoredDaySoAPartlyImportedDayIsCompleted() {
		when(fxRates.findLatestRateDate()).thenReturn(Optional.of(MONDAY));

		jobAt(THURSDAY_MORNING).run(run);

		verify(provider).ratesBetween(MONDAY, TestFx.THURSDAY);
	}

	@Test
	void aDayWithoutAPublishedTableHasNoItems() {
		when(fxRates.findLatestRateDate()).thenReturn(Optional.of(TestFx.THURSDAY));

		jobAt(THURSDAY_MORNING).run(run);

		assertThat(run.total).isZero();
		assertThat(run.outcomes).isEmpty();
	}

	@Test
	void countsTheDaysInWarsaw() {
		when(fxRates.findLatestRateDate()).thenReturn(Optional.of(TestFx.THURSDAY));

		jobAt(THURSDAY_LATE_EVENING_IN_UTC).run(run);

		verify(provider).ratesBetween(TestFx.THURSDAY, FRIDAY);
	}

	@Test
	void importsOneItemPerActiveForeignCurrencyAndDayAndSkipsTheRest() {
		PublishedFxRate euro = TestFx.published(TestFx.EUR, TestFx.THURSDAY, TestFx.EUR_MID);
		PublishedFxRate dollar = TestFx.published(TestFx.USD, TestFx.THURSDAY, TestFx.USD_MID);
		PublishedFxRate peso = TestFx.published(TestFx.CLP, TestFx.THURSDAY, TestFx.USD_MID);
		givenPublished(dollar, peso, euro);

		jobAt(THURSDAY_MORNING).run(run);

		assertThat(run.total).isEqualTo(run.outcomes.size());
		assertThat(run.outcomes).containsOnlyKeys(key(TestFx.EUR), key(TestFx.USD)).containsValues(RecordingRun.SUCCEEDED);
		verify(importService).store(euro);
		verify(importService).store(dollar);
		verify(importService, never()).store(peso);
	}

	@Test
	void aCurrencyMissingFromAPublishedTableIsAFailedItem() {
		givenPublished(TestFx.published(TestFx.USD, TestFx.THURSDAY, TestFx.USD_MID));

		jobAt(THURSDAY_MORNING).run(run);

		assertThat(run.outcomes).containsEntry(key(TestFx.EUR), FxError.RATE_MISSING_FROM_SOURCE.code()).containsEntry(key(TestFx.USD), RecordingRun.SUCCEEDED);
	}

	private FxRatesNbpJob jobAt(Instant now) {
		return new FxRatesNbpJob(provider, fxRates, currencies, importService, TestFx.properties(), Clock.fixed(now, ZoneOffset.UTC));
	}

	private void givenPublished(PublishedFxRate... rates) {
		when(provider.ratesBetween(any(LocalDate.class), any(LocalDate.class))).thenReturn(List.of(rates));
	}

	private static String key(String currency) {
		return currency + KEY_SEPARATOR + TestFx.THURSDAY;
	}

	private static final class RecordingRun implements BatchRun {

		static final String SUCCEEDED = "SUCCEEDED";

		private final Map<String, String> outcomes = new LinkedHashMap<>();
		private int total;

		@Override
		public void totalItems(int total) {
			this.total = total;
		}

		@Override
		public void item(String key, BatchItemWork work) {
			try {
				work.process();
				outcomes.put(key, SUCCEEDED);
			}
			catch (AppException failure) {
				outcomes.put(key, failure.errorCode().code());
			}
		}
	}
}
