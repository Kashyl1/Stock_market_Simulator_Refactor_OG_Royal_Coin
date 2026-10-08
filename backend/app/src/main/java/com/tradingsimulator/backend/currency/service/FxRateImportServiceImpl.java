package com.tradingsimulator.backend.currency.service;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tradingsimulator.backend.currency.FxRate;
import com.tradingsimulator.backend.currency.FxRateRepository;
import com.tradingsimulator.backend.currency.provider.PublishedFxRate;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FxRateImportServiceImpl implements FxRateImportService {

	private final FxRateRepository fxRates;

	@Override
	@Transactional
	public FxRateImport store(PublishedFxRate published) {
		Optional<FxRate> stored = fxRates.findForPairAndDay(published.baseCurrency(), published.quoteCurrency(), published.rateDate());
		if (stored.isEmpty()) {
			fxRates.save(FxRate.published(published));
			return FxRateImport.of(published, FxRateImportOutcome.INSERTED);
		}
		FxRateImportOutcome outcome = stored.get().correctTo(published.rate()) ? FxRateImportOutcome.CORRECTED : FxRateImportOutcome.UNCHANGED;
		return FxRateImport.of(published, outcome);
	}
}
