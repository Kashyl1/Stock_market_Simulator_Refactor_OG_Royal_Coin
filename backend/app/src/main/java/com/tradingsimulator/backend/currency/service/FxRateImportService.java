package com.tradingsimulator.backend.currency.service;

import com.tradingsimulator.backend.currency.provider.PublishedFxRate;

public interface FxRateImportService {

	FxRateImport store(PublishedFxRate published);
}
