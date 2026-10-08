package com.tradingsimulator.backend.currency.service;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface FxConversionService {

	BigDecimal rate(String from, String to, LocalDate day);

	BigDecimal convert(BigDecimal amount, String from, String to, LocalDate day);
}
