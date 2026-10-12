package com.tradingsimulator.backend.portfolio;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.tradingsimulator.backend.support.TestFx;
import com.tradingsimulator.backend.support.TestMoney;

class PerformanceTest {

	private static final BigDecimal TAKEN_OUT = new BigDecimal("350.00");
	private static final BigDecimal TWICE = BigDecimal.valueOf(2);

	@Test
	void holdingDollarsBoughtWithTheFeeShowsTheFeeAsALoss() {
		Performance performance = Performance.of(TestFx.PLN, new BigDecimal("398.00"), TestMoney.DEPOSITED_ZLOTY, BigDecimal.ZERO);

		assertThat(performance.result()).isEqualByComparingTo("-2.00");
		assertThat(performance.resultPercent()).isEqualByComparingTo("-0.50");
		assertThat(performance.currency()).isEqualTo(TestFx.PLN);
	}

	@Test
	void whatWasTakenOutCountsTowardsTheResult() {
		Performance performance = Performance.of(TestFx.PLN, new BigDecimal("100.00"), TestMoney.DEPOSITED_ZLOTY, TAKEN_OUT);

		assertThat(performance.netDeposits()).isEqualByComparingTo(TestMoney.DEPOSITED_ZLOTY.subtract(TAKEN_OUT));
		assertThat(performance.result()).isEqualByComparingTo("50.00");
		assertThat(performance.resultPercent()).isEqualByComparingTo("12.50");
	}

	@Test
	void takingMoneyOutAfterAGainDoesNotPumpThePercentage() {
		BigDecimal doubled = TestMoney.DEPOSITED_ZLOTY.multiply(TWICE);
		Performance held = Performance.of(TestFx.PLN, doubled, TestMoney.DEPOSITED_ZLOTY, BigDecimal.ZERO);
		Performance mostlyTakenOut = Performance.of(TestFx.PLN, doubled.subtract(TAKEN_OUT), TestMoney.DEPOSITED_ZLOTY, TAKEN_OUT);

		assertThat(mostlyTakenOut.result()).isEqualByComparingTo(held.result());
		assertThat(mostlyTakenOut.resultPercent()).isEqualByComparingTo(held.resultPercent());
	}

	@Test
	void withoutAValueThereIsNoResult() {
		Performance performance = Performance.of(TestFx.PLN, null, TestMoney.DEPOSITED_ZLOTY, BigDecimal.ZERO);

		assertThat(performance.value()).isNull();
		assertThat(performance.result()).isNull();
		assertThat(performance.resultPercent()).isNull();
		assertThat(performance.putIn()).isEqualByComparingTo(TestMoney.DEPOSITED_ZLOTY);
		assertThat(performance.netDeposits()).isEqualByComparingTo(TestMoney.DEPOSITED_ZLOTY);
	}

	@Test
	void nothingPutInHasNoPercentage() {
		Performance performance = Performance.of(TestFx.PLN, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);

		assertThat(performance.result()).isEqualByComparingTo(BigDecimal.ZERO);
		assertThat(performance.resultPercent()).isNull();
	}
}
