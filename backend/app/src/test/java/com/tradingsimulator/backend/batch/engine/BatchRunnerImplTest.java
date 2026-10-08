package com.tradingsimulator.backend.batch.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.PlatformTransactionManager;

import com.tradingsimulator.backend.batch.BatchError;
import com.tradingsimulator.backend.batch.BatchItem;
import com.tradingsimulator.backend.batch.BatchItemRepository;
import com.tradingsimulator.backend.batch.BatchItemStatus;
import com.tradingsimulator.backend.batch.BatchJob;
import com.tradingsimulator.backend.batch.BatchJobRepository;
import com.tradingsimulator.backend.batch.BatchJobStatus;
import com.tradingsimulator.backend.batch.BatchStopRequest;
import com.tradingsimulator.backend.batch.BatchTypeCode;
import com.tradingsimulator.backend.batch.BatchTypeRepository;
import com.tradingsimulator.backend.common.error.AppException;
import com.tradingsimulator.backend.common.error.CommonError;
import com.tradingsimulator.backend.support.TestBatch;

import tools.jackson.databind.json.JsonMapper;

class BatchRunnerImplTest {

	private static final long JOB_ID = 9L;
	private static final Instant NOW = TestBatch.AT_10_00.plusSeconds(30);
	private static final String DOLLAR = "USD";
	private static final String USD = DOLLAR + " 2026-10-08";
	private static final String EUR = "EUR 2026-10-08";
	private static final String JPY = "JPY 2026-10-08";
	private static final BigDecimal USD_MID = new BigDecimal("3.9132");
	private static final String USD_PAYLOAD = "{\"currency\":\"" + DOLLAR + "\",\"rate\":" + USD_MID + "}";
	private static final String BROKEN = "table without USD";
	private static final String ADMIN = "user:1";
	private static final int TWO = 2;
	private static final int ONE = 1;

	private final BatchJobRepository jobs = mock(BatchJobRepository.class);
	private final BatchTypeRepository types = mock(BatchTypeRepository.class);
	private final BatchItemRepository items = mock(BatchItemRepository.class);
	private final RunningBatchJobs running = new RunningBatchJobs();
	private final BatchJob job = TestBatch.runningJob(JOB_ID, TestBatch.AT_10_00);

	@BeforeEach
	void stubJob() {
		when(jobs.findById(JOB_ID)).thenReturn(Optional.of(job));
		when(types.findById(TestBatch.TYPE_ID)).thenReturn(Optional.of(TestBatch.hourlyType()));
	}

	@Test
	void aJobWhoseItemsAllSucceedIsCompleted() {
		runnerWith(run -> {
			run.totalItems(TWO);
			run.item(USD, () -> new RatePayload(DOLLAR, USD_MID));
			run.item(EUR, () -> null);
		}).run(JOB_ID);

		assertThat(job.getStatus()).isEqualTo(BatchJobStatus.COMPLETED);
		assertThat(job.getTotalItems()).isEqualTo(TWO);
		assertThat(job.getSucceededItems()).isEqualTo(TWO);
		assertThat(job.getFailedItems()).isZero();
		assertThat(job.getFinishedAt()).isEqualTo(NOW);
		assertThat(savedItems()).extracting(BatchItem::getItemKey, BatchItem::getStatus, BatchItem::getPayload)
				.containsExactly(tuple(USD, BatchItemStatus.SUCCEEDED, USD_PAYLOAD), tuple(EUR, BatchItemStatus.SUCCEEDED, null));
	}

	@Test
	void aFailingItemIsRecordedWithItsErrorAndTheOthersCarryOn() {
		runnerWith(run -> {
			run.item(USD, () -> {
				throw new IllegalStateException(BROKEN);
			});
			run.item(EUR, () -> null);
			run.item(JPY, () -> null);
		}).run(JOB_ID);

		assertThat(job.getStatus()).isEqualTo(BatchJobStatus.COMPLETED_WITH_ERRORS);
		assertThat(job.getSucceededItems()).isEqualTo(TWO);
		assertThat(job.getFailedItems()).isEqualTo(ONE);
		BatchItem failed = savedItems().getFirst();
		assertThat(failed.getStatus()).isEqualTo(BatchItemStatus.FAILED);
		assertThat(failed.getErrorCode()).isEqualTo(CommonError.INTERNAL_ERROR.code());
		assertThat(failed.getErrorMessage()).contains(IllegalStateException.class.getSimpleName()).contains(BROKEN);
	}

	@Test
	void aJobThatBreaksOutsideItsItemsFails() {
		runnerWith(run -> {
			run.item(USD, () -> null);
			throw new AppException(BatchError.TYPE_DISABLED, BatchTypeCode.FX_RATES_NBP);
		}).run(JOB_ID);

		assertThat(job.getStatus()).isEqualTo(BatchJobStatus.FAILED);
		assertThat(job.getErrorCode()).isEqualTo(BatchError.TYPE_DISABLED.code());
		assertThat(job.getSucceededItems()).isEqualTo(ONE);
	}

	@Test
	void aStopRequestEndsTheJobBeforeItsNextItem() {
		AtomicBoolean secondItemRan = new AtomicBoolean();
		BatchStopRequest stop = new BatchStopRequest(ADMIN, NOW);

		runnerWith(run -> {
			run.item(USD, () -> null);
			running.requestStop(JOB_ID, stop);
			run.item(EUR, () -> secondItemRan.getAndSet(true));
		}).run(JOB_ID);

		assertThat(secondItemRan).isFalse();
		assertThat(job.getStatus()).isEqualTo(BatchJobStatus.STOPPED);
		assertThat(job.getStopRequestedBy()).isEqualTo(ADMIN);
		assertThat(job.getSucceededItems()).isEqualTo(ONE);
		assertThat(running.stopRequest(JOB_ID)).isEmpty();
	}

	@Test
	void aTypeWithoutAHandlerFails() {
		runner(List.of()).run(JOB_ID);

		assertThat(job.getStatus()).isEqualTo(BatchJobStatus.FAILED);
		assertThat(job.getErrorCode()).isEqualTo(BatchError.HANDLER_MISSING.code());
	}

	private BatchRunner runnerWith(Consumer<BatchRun> script) {
		return runner(List.of(new ScriptedHandler(script)));
	}

	private BatchRunner runner(List<BatchJobHandler> handlers) {
		Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
		return new BatchRunnerImpl(jobs, types, items, running, handlers, mock(PlatformTransactionManager.class), JsonMapper.builder().build(), clock);
	}

	private List<BatchItem> savedItems() {
		ArgumentCaptor<BatchItem> saved = ArgumentCaptor.forClass(BatchItem.class);
		verify(items, atLeastOnce()).save(saved.capture());
		return saved.getAllValues();
	}

	private record RatePayload(String currency, BigDecimal rate) {
	}

	private record ScriptedHandler(Consumer<BatchRun> script) implements BatchJobHandler {

		@Override
		public BatchTypeCode type() {
			return BatchTypeCode.FX_RATES_NBP;
		}

		@Override
		public void run(BatchRun run) {
			script.accept(run);
		}
	}
}
