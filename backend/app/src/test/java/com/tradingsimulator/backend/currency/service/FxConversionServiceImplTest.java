package com.tradingsimulator.backend.currency.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.tradingsimulator.backend.common.error.AppException;
import com.tradingsimulator.backend.currency.Currency;
import com.tradingsimulator.backend.currency.CurrencyRepository;
import com.tradingsimulator.backend.currency.FxError;
import com.tradingsimulator.backend.currency.FxRate;
import com.tradingsimulator.backend.currency.FxRateRepository;
import com.tradingsimulator.backend.support.TestFx;

class FxConversionServiceImplTest {

	private static final String UNKNOWN = "XYZ";
	private static final BigDecimal THOUSAND = new BigDecimal("1000");
	private static final BigDecimal HUNDRED = new BigDecimal("100");
	private static final BigDecimal THOUSAND_DOLLARS_IN_YEN = new BigDecimal("158243");
	private static final BigDecimal HUNDRED_ZLOTY_IN_DOLLARS = new BigDecimal("25.55");
	private static final BigDecimal UNROUNDED_ZLOTY = new BigDecimal("100.005");
	private static final BigDecimal ROUNDED_ZLOTY = new BigDecimal("100.01");

	private final CurrencyRepository currencies = mock(CurrencyRepository.class);
	private final FxRateRepository fxRates = mock(FxRateRepository.class);
	private final FxConversionService service = new FxConversionServiceImpl(currencies, fxRates);

	@BeforeEach
	void stubRates() {
		givenCurrency(TestFx.currency(TestFx.PLN, TestFx.CENTS));
		givenCurrency(TestFx.currency(TestFx.USD, TestFx.CENTS));
		givenCurrency(TestFx.currency(TestFx.EUR, TestFx.CENTS));
		givenCurrency(TestFx.currency(TestFx.JPY, TestFx.WHOLE_UNITS));
		givenRate(TestFx.USD, TestFx.USD_MID);
		givenRate(TestFx.EUR, TestFx.EUR_MID);
		givenRate(TestFx.JPY, TestFx.JPY_MID);
	}

	@Test
	void aCurrencyIsWorthItselfAndAnAmountIsOnlyRounded() {
		assertThat(service.rate(TestFx.PLN, TestFx.PLN, TestFx.THURSDAY)).isEqualTo(BigDecimal.ONE);
		assertThat(service.convert(UNROUNDED_ZLOTY, TestFx.PLN, TestFx.PLN, TestFx.THURSDAY)).isEqualTo(ROUNDED_ZLOTY);
	}

	@Test
	void aForeignCurrencyInZlotyIsTheNbpMidRate() {
		assertThat(service.rate(TestFx.USD, TestFx.PLN, TestFx.THURSDAY)).isEqualByComparingTo(TestFx.USD_MID);
	}

	@Test
	void zlotyInAForeignCurrencyIsTheInverse() {
		BigDecimal inverse = BigDecimal.ONE.divide(TestFx.USD_MID, FxRate.RATE_SCALE, Currency.ROUNDING_MODE);

		assertThat(service.rate(TestFx.PLN, TestFx.USD, TestFx.THURSDAY)).isEqualTo(inverse);
	}

	@Test
	void aCrossRateGoesThroughTheZloty() {
		BigDecimal cross = TestFx.EUR_MID.divide(TestFx.USD_MID, FxRate.RATE_SCALE, Currency.ROUNDING_MODE);

		assertThat(service.rate(TestFx.EUR, TestFx.USD, TestFx.THURSDAY)).isEqualTo(cross);
	}

	@Test
	void aConvertedAmountIsRoundedToTheMinorUnitsOfItsCurrency() {
		assertThat(service.convert(THOUSAND, TestFx.USD, TestFx.JPY, TestFx.THURSDAY)).isEqualTo(THOUSAND_DOLLARS_IN_YEN);
		assertThat(service.convert(HUNDRED, TestFx.PLN, TestFx.USD, TestFx.THURSDAY)).isEqualTo(HUNDRED_ZLOTY_IN_DOLLARS);
	}

	@Test
	void anUnknownCurrencyIsRejected() {
		assertFailsWith(() -> service.rate(UNKNOWN, TestFx.PLN, TestFx.THURSDAY), FxError.UNKNOWN_CURRENCY);
	}

	@Test
	void aDayBeforeTheFirstStoredRateHasNoRate() {
		when(fxRates.findLatestOnOrBefore(TestFx.USD, TestFx.PLN, TestFx.WEDNESDAY)).thenReturn(Optional.empty());

		assertFailsWith(() -> service.rate(TestFx.USD, TestFx.PLN, TestFx.WEDNESDAY), FxError.RATE_NOT_AVAILABLE);
	}

	private void givenCurrency(Currency currency) {
		when(currencies.findByCodeAndActiveTrue(currency.getCode())).thenReturn(Optional.of(currency));
	}

	private void givenRate(String currency, BigDecimal mid) {
		when(fxRates.findLatestOnOrBefore(currency, TestFx.PLN, TestFx.THURSDAY)).thenReturn(Optional.of(TestFx.stored(currency, TestFx.THURSDAY, mid)));
	}

	private static void assertFailsWith(ThrowingCallable call, FxError expected) {
		assertThatThrownBy(call).isInstanceOf(AppException.class).extracting(failure -> ((AppException) failure).errorCode()).isEqualTo(expected);
	}
}
