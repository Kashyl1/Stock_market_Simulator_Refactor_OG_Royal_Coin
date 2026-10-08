package com.tradingsimulator.backend.currency.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.tradingsimulator.backend.currency.FxRate;
import com.tradingsimulator.backend.currency.FxRateRepository;
import com.tradingsimulator.backend.currency.provider.PublishedFxRate;
import com.tradingsimulator.backend.support.TestFx;

class FxRateImportServiceImplTest {

	private static final int STORED_SCALE = 18;
	private static final BigDecimal CORRECTED_MID = new BigDecimal("3.9140");

	private final FxRateRepository fxRates = mock(FxRateRepository.class);
	private final FxRateImportService service = new FxRateImportServiceImpl(fxRates);
	private final PublishedFxRate published = TestFx.published(TestFx.USD, TestFx.THURSDAY, TestFx.USD_MID);

	@Test
	void insertsARateNotStoredYet() {
		FxRateImport imported = service.store(published);

		assertThat(imported.outcome()).isEqualTo(FxRateImportOutcome.INSERTED);
		verify(fxRates).save(any(FxRate.class));
	}

	@Test
	void leavesAnEqualRateAloneWhateverItsScale() {
		FxRate stored = TestFx.stored(TestFx.USD, TestFx.THURSDAY, TestFx.USD_MID.setScale(STORED_SCALE, RoundingMode.UNNECESSARY));
		givenStored(stored);

		assertThat(service.store(published).outcome()).isEqualTo(FxRateImportOutcome.UNCHANGED);
		verify(fxRates, never()).save(any(FxRate.class));
	}

	@Test
	void correctsARateNbpChanged() {
		FxRate stored = TestFx.stored(TestFx.USD, TestFx.THURSDAY, TestFx.USD_MID);
		givenStored(stored);

		FxRateImport imported = service.store(TestFx.published(TestFx.USD, TestFx.THURSDAY, CORRECTED_MID));

		assertThat(imported.outcome()).isEqualTo(FxRateImportOutcome.CORRECTED);
		assertThat(stored.getRate()).isEqualTo(CORRECTED_MID);
	}

	private void givenStored(FxRate stored) {
		when(fxRates.findForPairAndDay(TestFx.USD, TestFx.PLN, TestFx.THURSDAY)).thenReturn(Optional.of(stored));
	}
}
