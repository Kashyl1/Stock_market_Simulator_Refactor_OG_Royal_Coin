package com.tradingsimulator.backend.currency.service;

import com.tradingsimulator.backend.currency.Currency;

public record CurrencyView(String code, String name, int minorUnits) {

	static CurrencyView of(Currency currency) {
		return new CurrencyView(currency.getCode(), currency.getName(), currency.getMinorUnits());
	}
}
