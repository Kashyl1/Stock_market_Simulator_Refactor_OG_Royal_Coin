package com.tradingsimulator.backend.currency;

import java.math.BigDecimal;
import java.math.RoundingMode;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "currency")
public class Currency extends CurrencyJpa {

	public static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

	public static Currency active(String code, String name, int minorUnits) {
		Currency currency = new Currency();
		currency.setCode(code);
		currency.setName(name);
		currency.setMinorUnits(minorUnits);
		currency.setActive(true);
		return currency;
	}

	public BigDecimal round(BigDecimal amount) {
		return amount.setScale(getMinorUnits(), ROUNDING_MODE);
	}
}
