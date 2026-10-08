package com.tradingsimulator.backend.currency.provider.nbp;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.tradingsimulator.backend.common.error.AppException;
import com.tradingsimulator.backend.currency.FxError;
import com.tradingsimulator.backend.currency.FxProperties;
import com.tradingsimulator.backend.currency.FxRate;
import com.tradingsimulator.backend.currency.FxRateSource;
import com.tradingsimulator.backend.currency.provider.FxRateProvider;
import com.tradingsimulator.backend.currency.provider.PublishedFxRate;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class NbpFxRateProvider implements FxRateProvider {

	private static final String TABLE_A_BETWEEN = "/exchangerates/tables/A/{from}/{to}/?format=json";

	private final RestClient nbpRestClient;
	private final FxProperties properties;

	@Override
	public List<PublishedFxRate> ratesBetween(LocalDate from, LocalDate to) {
		int daysPerRequest = properties.nbp().daysPerRequest();
		List<PublishedFxRate> rates = new ArrayList<>();
		for (LocalDate start = from; !start.isAfter(to); start = start.plusDays(daysPerRequest)) {
			LocalDate lastDayOfRequest = start.plusDays(daysPerRequest - 1L);
			LocalDate end = lastDayOfRequest.isBefore(to) ? lastDayOfRequest : to;
			tablesBetween(start, end).forEach(table -> table.rates().forEach(rate -> rates.add(published(table, rate))));
		}
		return List.copyOf(rates);
	}

	private List<NbpTable> tablesBetween(LocalDate from, LocalDate to) {
		try {
			NbpTable[] tables = nbpRestClient.get().uri(TABLE_A_BETWEEN, from, to).retrieve().body(NbpTable[].class);
			return tables == null ? List.of() : List.of(tables);
		}
		catch (HttpClientErrorException.NotFound noTableInRange) {
			return List.of();
		}
		catch (RestClientException unavailable) {
			throw new AppException(FxError.PROVIDER_UNAVAILABLE, unavailable, FxRateSource.NBP);
		}
	}

	private static PublishedFxRate published(NbpTable table, NbpRate rate) {
		return new PublishedFxRate(rate.code(), FxRate.PIVOT_CURRENCY, table.effectiveDate(), rate.midRate(), FxRateSource.NBP);
	}
}
