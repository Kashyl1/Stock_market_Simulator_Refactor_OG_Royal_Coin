package com.tradingsimulator.backend.portfolio;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.tradingsimulator.backend.currency.Currency;
import com.tradingsimulator.backend.support.TestFx;
import com.tradingsimulator.backend.support.TestMoney;

class TransferQuoteTest {

	private static final Currency ZLOTY = TestFx.currency(TestFx.PLN, TestFx.CENTS);
	private static final Currency DOLLAR = TestFx.currency(TestFx.USD, TestFx.CENTS);
	private static final Currency YEN = TestFx.currency(TestFx.JPY, TestFx.WHOLE_UNITS);
	private static final ConversionTerms ZLOTY_TO_DOLLAR = new ConversionTerms(ZLOTY, DOLLAR, TestMoney.DOLLARS_PER_ZLOTY, TestMoney.FX_CONVERSION_RATE);
	private static final ConversionTerms ZLOTY_TO_YEN = new ConversionTerms(ZLOTY, YEN, TestMoney.YEN_PER_ZLOTY, TestMoney.FX_CONVERSION_RATE);
	private static final BigDecimal SAME_CURRENCY_AMOUNT = new BigDecimal("300.00");

	@Test
	void aDepositIntoADollarPortfolioPaysTheFeeInZlotyAndConvertsTheRest() {
		TransferQuote quote = TransferQuote.of(TransferDirection.IN, TestMoney.DEPOSITED_ZLOTY, ZLOTY_TO_DOLLAR);

		assertThat(quote.mainAmount()).isEqualByComparingTo(TestMoney.DEPOSITED_ZLOTY);
		assertThat(quote.fxFeeAmount()).isEqualByComparingTo("2.00");
		assertThat(quote.amount()).isEqualByComparingTo(TestMoney.ARRIVED_DOLLARS);
		assertThat(quote.fxRate()).isEqualByComparingTo(TestMoney.DOLLARS_PER_ZLOTY);
		assertThat(quote.leaving()).isEqualByComparingTo(TestMoney.DEPOSITED_ZLOTY);
		assertThat(quote.arriving()).isEqualByComparingTo(quote.amount());
	}

	@Test
	void aWithdrawalConvertsTheDollarsBackAndPaysTheFeeInZloty() {
		TransferQuote quote = TransferQuote.of(TransferDirection.OUT, TestMoney.WITHDRAWN_DOLLARS, ZLOTY_TO_DOLLAR);

		assertThat(quote.amount()).isEqualByComparingTo(TestMoney.WITHDRAWN_DOLLARS);
		assertThat(quote.fxFeeAmount()).isEqualByComparingTo("1.00");
		assertThat(quote.mainAmount()).isEqualByComparingTo("199.00");
		assertThat(quote.leaving()).isEqualByComparingTo(TestMoney.WITHDRAWN_DOLLARS);
		assertThat(quote.arriving()).isEqualByComparingTo(quote.mainAmount());
	}

	@Test
	void theSameCurrencyMovesWithoutARateOrAFee() {
		TransferQuote quote = TransferQuote.of(TransferDirection.IN, SAME_CURRENCY_AMOUNT, ConversionTerms.sameCurrency(ZLOTY));

		assertThat(quote.amount()).isEqualByComparingTo(SAME_CURRENCY_AMOUNT);
		assertThat(quote.mainAmount()).isEqualByComparingTo(SAME_CURRENCY_AMOUNT);
		assertThat(quote.fxRate()).isNull();
		assertThat(quote.fxFeeAmount()).isEqualByComparingTo(BigDecimal.ZERO);
	}

	@Test
	void yenArriveInWholeUnits() {
		TransferQuote quote = TransferQuote.of(TransferDirection.IN, new BigDecimal("10.00"), ZLOTY_TO_YEN);

		assertThat(quote.fxFeeAmount()).isEqualByComparingTo("0.05");
		assertThat(quote.amount()).isEqualTo(new BigDecimal("402"));
	}

	@Test
	void aGroszIntoYenArrivesAsNothing() {
		assertThat(TransferQuote.of(TransferDirection.IN, new BigDecimal("0.01"), ZLOTY_TO_YEN).arrivesAsNothing()).isTrue();
		assertThat(TransferQuote.of(TransferDirection.IN, TestMoney.DEPOSITED_ZLOTY, ZLOTY_TO_YEN).arrivesAsNothing()).isFalse();
	}
}
