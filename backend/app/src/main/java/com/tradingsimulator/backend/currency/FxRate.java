package com.tradingsimulator.backend.currency;

import java.math.BigDecimal;

import com.tradingsimulator.backend.currency.provider.PublishedFxRate;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "fx_rate")
public class FxRate extends FxRateJpa {

	public static final String PIVOT_CURRENCY = "PLN";

	public static FxRate published(PublishedFxRate published) {
		FxRate rate = new FxRate();
		rate.setBaseCurrency(published.baseCurrency());
		rate.setQuoteCurrency(published.quoteCurrency());
		rate.setRateDate(published.rateDate());
		rate.setRate(published.rate());
		rate.setSource(published.source());
		return rate;
	}

	public boolean correctTo(BigDecimal publishedRate) {
		if (getRate().compareTo(publishedRate) == 0) {
			return false;
		}
		setRate(publishedRate);
		return true;
	}
}
