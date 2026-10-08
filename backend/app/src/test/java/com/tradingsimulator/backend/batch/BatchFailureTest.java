package com.tradingsimulator.backend.batch;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.tradingsimulator.backend.common.error.AppException;
import com.tradingsimulator.backend.common.error.CommonError;

class BatchFailureTest {

	private static final String BROKEN = "connection refused";
	private static final String FILLER = "x";

	@Test
	void keepsTheCodeAndMessageOfOurOwnError() {
		BatchFailure failure = BatchFailure.of(new AppException(BatchError.HANDLER_MISSING, BatchTypeCode.FX_RATES_NBP));

		assertThat(failure.code()).isEqualTo(BatchError.HANDLER_MISSING.code());
		assertThat(failure.message()).contains(BatchTypeCode.FX_RATES_NBP.name());
	}

	@Test
	void namesTheTypeOfAnUnexpectedError() {
		BatchFailure failure = BatchFailure.of(new IllegalStateException(BROKEN));

		assertThat(failure.code()).isEqualTo(CommonError.INTERNAL_ERROR.code());
		assertThat(failure.message()).startsWith(IllegalStateException.class.getSimpleName()).endsWith(BROKEN);
	}

	@Test
	void cutsALongMessageToTheColumnLength() {
		BatchFailure failure = BatchFailure.of(new IllegalStateException(FILLER.repeat(BatchFailure.MESSAGE_MAX_LENGTH * 2)));

		assertThat(failure.message()).hasSize(BatchFailure.MESSAGE_MAX_LENGTH);
	}
}
