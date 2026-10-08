package com.tradingsimulator.backend.batch.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.AuditorAware;

import com.tradingsimulator.backend.batch.BatchError;
import com.tradingsimulator.backend.batch.BatchFailure;
import com.tradingsimulator.backend.batch.BatchItemCounts;
import com.tradingsimulator.backend.batch.BatchItemRepository;
import com.tradingsimulator.backend.batch.BatchJob;
import com.tradingsimulator.backend.batch.BatchJobOrigin;
import com.tradingsimulator.backend.batch.BatchJobRepository;
import com.tradingsimulator.backend.batch.BatchJobStatus;
import com.tradingsimulator.backend.batch.BatchType;
import com.tradingsimulator.backend.batch.BatchTypeCode;
import com.tradingsimulator.backend.batch.BatchTypeRepository;
import com.tradingsimulator.backend.batch.engine.BatchPlanner;
import com.tradingsimulator.backend.batch.engine.RunningBatchJobs;
import com.tradingsimulator.backend.common.error.AppException;
import com.tradingsimulator.backend.common.error.ErrorCode;
import com.tradingsimulator.backend.support.TestBatch;

class BatchAdminServiceImplTest {

	private static final Instant NOW = TestBatch.AT_10_00;
	private static final Instant LATER = TestBatch.AT_12_00;
	private static final Instant EARLIER = NOW.minusSeconds(60);
	private static final String ADMIN = "user:1";
	private static final long JOB_ID = 5L;
	private static final int SUCCEEDED_SO_FAR = 5;
	private static final int FAILED_SO_FAR = 1;

	private final BatchTypeRepository types = mock(BatchTypeRepository.class);
	private final BatchJobRepository jobs = mock(BatchJobRepository.class);
	private final BatchItemRepository items = mock(BatchItemRepository.class);
	private final BatchPlanner planner = mock(BatchPlanner.class);
	private final RunningBatchJobs running = new RunningBatchJobs();
	private final AuditorAware<String> auditorAware = () -> Optional.of(ADMIN);
	private final BatchAdminService service = new BatchAdminServiceImpl(types, jobs, items, planner, running, auditorAware, Clock.fixed(NOW, ZoneOffset.UTC));

	@BeforeEach
	void stubTypes() {
		when(types.findByCode(BatchTypeCode.FX_RATES_NBP)).thenReturn(Optional.of(TestBatch.hourlyType()));
		when(types.findAll()).thenReturn(List.of(TestBatch.hourlyType()));
		when(jobs.save(any(BatchJob.class))).thenAnswer(invocation -> invocation.getArgument(0));
	}

	@Test
	void plansAManualJobForNowWhenNoTimeIsGiven() {
		BatchJobView planned = service.plan(BatchTypeCode.FX_RATES_NBP, null);

		assertThat(planned.status()).isEqualTo(BatchJobStatus.SCHEDULED);
		assertThat(planned.origin()).isEqualTo(BatchJobOrigin.MANUAL);
		assertThat(planned.type()).isEqualTo(BatchTypeCode.FX_RATES_NBP);
		assertThat(planned.scheduledFor()).isEqualTo(NOW);
	}

	@Test
	void plansAManualJobForTheRequestedTime() {
		assertThat(service.plan(BatchTypeCode.FX_RATES_NBP, LATER).scheduledFor()).isEqualTo(LATER);
	}

	@Test
	void aTimeInThePastMeansNow() {
		assertThat(service.plan(BatchTypeCode.FX_RATES_NBP, EARLIER).scheduledFor()).isEqualTo(NOW);
	}

	@Test
	void aSwitchedOffTypeCannotBePlanned() {
		BatchType switchedOff = TestBatch.hourlyType();
		switchedOff.setEnabled(false);
		when(types.findByCode(BatchTypeCode.FX_RATES_NBP)).thenReturn(Optional.of(switchedOff));

		assertFailsWith(() -> service.plan(BatchTypeCode.FX_RATES_NBP, null), BatchError.TYPE_DISABLED);
	}

	@Test
	void removingAJobIsThePlannersJob() {
		service.remove(JOB_ID);

		verify(planner).remove(JOB_ID);
	}

	@Test
	void stopsARunningJobInTheNameOfTheAdmin() {
		givenJob(TestBatch.runningJob(JOB_ID, NOW));
		running.register(JOB_ID);

		service.stop(JOB_ID);

		assertThat(running.stopRequest(JOB_ID)).hasValueSatisfying(request -> assertThat(request.requestedBy()).isEqualTo(ADMIN));
	}

	@Test
	void onlyARunningJobCanBeStopped() {
		givenJob(TestBatch.manualJob(JOB_ID, LATER));

		assertFailsWith(() -> service.stop(JOB_ID), BatchError.JOB_NOT_RUNNING);
	}

	@Test
	void aJobMarkedRunningThatThisApplicationIsNotRunningCannotBeStopped() {
		givenJob(TestBatch.runningJob(JOB_ID, NOW));

		assertFailsWith(() -> service.stop(JOB_ID), BatchError.JOB_NOT_ACTIVE);
	}

	@Test
	void acknowledgesAFailureReportInTheNameOfTheAdmin() {
		givenJob(failedJob());

		BatchJobView acknowledged = service.acknowledge(JOB_ID);

		assertThat(acknowledged.acknowledgedBy()).isEqualTo(ADMIN);
		assertThat(acknowledged.acknowledgedAt()).isEqualTo(NOW);
	}

	@Test
	void aJobWithoutFailuresHasNothingToAcknowledge() {
		BatchJob completed = TestBatch.runningJob(JOB_ID, NOW);
		completed.complete(new BatchItemCounts(SUCCEEDED_SO_FAR, 0), NOW);
		givenJob(completed);

		assertFailsWith(() -> service.acknowledge(JOB_ID), BatchError.JOB_WITHOUT_REPORT);
	}

	@Test
	void aRunningJobShowsTheItemsProcessedSoFar() {
		givenJob(TestBatch.runningJob(JOB_ID, NOW));
		when(items.countsOf(JOB_ID)).thenReturn(new BatchItemCounts(SUCCEEDED_SO_FAR, FAILED_SO_FAR));

		BatchJobView view = service.job(JOB_ID);

		assertThat(view.succeededItems()).isEqualTo(SUCCEEDED_SO_FAR);
		assertThat(view.failedItems()).isEqualTo(FAILED_SO_FAR);
	}

	@Test
	void anUnknownJobIsNotFound() {
		when(jobs.findById(JOB_ID)).thenReturn(Optional.empty());

		assertFailsWith(() -> service.job(JOB_ID), BatchError.JOB_NOT_FOUND);
	}

	private BatchJob failedJob() {
		BatchJob failed = TestBatch.runningJob(JOB_ID, NOW);
		failed.fail(new BatchItemCounts(0, FAILED_SO_FAR), BatchFailure.of(new AppException(BatchError.JOB_INTERRUPTED)), NOW);
		return failed;
	}

	private void givenJob(BatchJob job) {
		when(jobs.findById(JOB_ID)).thenReturn(Optional.of(job));
	}

	private static void assertFailsWith(ThrowingCallable call, ErrorCode expected) {
		assertThatThrownBy(call).isInstanceOf(AppException.class).extracting(failure -> ((AppException) failure).errorCode()).isEqualTo(expected);
	}
}
