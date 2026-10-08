package com.tradingsimulator.backend.currency.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tradingsimulator.backend.common.error.AppException;
import com.tradingsimulator.backend.currency.Currency;
import com.tradingsimulator.backend.currency.CurrencyRepository;
import com.tradingsimulator.backend.currency.FxError;
import com.tradingsimulator.backend.currency.FxRate;
import com.tradingsimulator.backend.currency.FxRateRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FxConversionServiceImpl implements FxConversionService {

	private final CurrencyRepository currencies;
	private final FxRateRepository fxRates;

	@Override
	@Transactional(readOnly = true)
	public BigDecimal rate(String from, String to, LocalDate day) {
		supported(from);
		supported(to);
		if (from.equals(to)) {
			return BigDecimal.ONE;
		}
		return rateInPivot(from, day).divide(rateInPivot(to, day), FxRate.RATE_SCALE, Currency.ROUNDING_MODE);
	}

	@Override
	@Transactional(readOnly = true)
	public BigDecimal convert(BigDecimal amount, String from, String to, LocalDate day) {
		return supported(to).round(amount.multiply(rate(from, to, day)));
	}

	private BigDecimal rateInPivot(String currency, LocalDate day) {
		if (FxRate.PIVOT_CURRENCY.equals(currency)) {
			return BigDecimal.ONE;
		}
		Optional<FxRate> latest = fxRates.findLatestOnOrBefore(currency, FxRate.PIVOT_CURRENCY, day);
		return latest.map(FxRate::getRate).orElseThrow(() -> new AppException(FxError.RATE_NOT_AVAILABLE, currency, day));
	}

	private Currency supported(String code) {
		return currencies.findByCodeAndActiveTrue(code).orElseThrow(() -> new AppException(FxError.UNKNOWN_CURRENCY, code));
	}
}
