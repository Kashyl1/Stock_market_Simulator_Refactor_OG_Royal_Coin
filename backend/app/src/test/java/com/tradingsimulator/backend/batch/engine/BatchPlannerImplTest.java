package com.tradingsimulator.backend.batch.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.tradingsimulator.backend.batch.BatchError;
import com.tradingsimulator.backend.batch.BatchItemCounts;
import com.tradingsimulator.backend.batch.BatchItemRepository;
import com.tradingsimulator.backend.batch.BatchJob;
import com.tradingsimulator.backend.batch.BatchJobOrigin;
import com.tradingsimulator.backend.batch.BatchJobRepository;
import com.tradingsimulator.backend.batch.BatchJobStatus;
import com.tradingsimulator.backend.batch.BatchTypeRepository;
import com.tradingsimulator.backend.common.error.AppException;
import com.tradingsimulator.backend.common.persistence.HistoryActor;
import com.tradingsimulator.backend.support.TestBatch;

class BatchPlannerImplTest {

	private static final Instant JUST_AFTER_10_00 = TestBatch.AT_10_00.plusSeconds(5);
	private static final Instant JUST_AFTER_10_02 = TestBatch.AT_10_02.plusSeconds(5);
	private static final Instant JUST_AFTER_11_00 = TestBatch.AT_11_00.plusSeconds(5);
	private static final Instant AFTER_DOWNTIME = Instant.parse("2026-10-08T15:20:00Z");
	private static final Instant NEXT_SLOT_AFTER_DOWNTIME = Instant.parse("2026-10-08T16:00:00Z");
	private static final Instant HALF_PAST_10 = Instant.parse("2026-10-08T10:30:00Z");
	private static final Instant QUIET_MIDNIGHT = Instant.parse("2026-10-09T00:00:00Z");
	private static final Instant TEN_TO_MIDNIGHT = QUIET_MIDNIGHT.minusSeconds(600);
	private static final Instant HALF_PAST_MIDNIGHT = Instant.parse("2026-10-09T00:30:00Z");
	private static final Instant ONE_AT_NIGHT = Instant.parse("2026-10-09T01:00:00Z");
	private static final Instant JUST_AFTER_THE_QUIET_WINDOW = Instant.parse("2026-10-09T02:31:00Z");
	private static final Instant THREE_AT_NIGHT = Instant.parse("2026-10-09T03:00:00Z");
	private static final long FIRST_JOB_ID = 1L;
	private static final long SECOND_JOB_ID = 2L;
	private static final int SUCCEEDED_BEFORE_CRASH = 3;
	private static final int FAILED_BEFORE_CRASH = 1;

	private final BatchTypeRepository types = mock(BatchTypeRepository.class);
	private final BatchJobRepository jobs = mock(BatchJobRepository.class);
	private final BatchItemRepository items = mock(BatchItemRepository.class);
	private final HistoryActor historyActor = mock(HistoryActor.class);

	@BeforeEach
	void stubTypes() {
		when(types.findAll()).thenReturn(List.of(TestBatch.hourlyType()));
		when(types.findById(TestBatch.TYPE_ID)).thenReturn(Optional.of(TestBatch.hourlyType()));
	}

	@Test
	void startsADueCronJobAndPlansTheNextSlot() {
		BatchJob due = TestBatch.cronJob(FIRST_JOB_ID, TestBatch.AT_10_00);
		givenDue(due);

		assertThat(plannerAt(JUST_AFTER_10_00).startDueJobs()).containsExactly(FIRST_JOB_ID);

		assertThat(due.getStatus()).isEqualTo(BatchJobStatus.RUNNING);
		assertThat(due.getStartedAt()).isEqualTo(JUST_AFTER_10_00);
		assertPlannedCronJobAt(TestBatch.AT_11_00);
	}

	@Test
	void deletesACronJobWhoseStartComesWhileItsTypeIsStillRunningAndPlansTheFollowingSlot() {
		givenRunning(TestBatch.runningJob(FIRST_JOB_ID, TestBatch.AT_10_00));
		BatchJob overrun = TestBatch.cronJob(SECOND_JOB_ID, TestBatch.AT_11_00);
		givenDue(overrun);

		assertThat(plannerAt(JUST_AFTER_11_00).startDueJobs()).isEmpty();

		verify(historyActor).declareCurrentAuditor();
		verify(jobs).delete(overrun);
		assertPlannedCronJobAt(TestBatch.AT_12_00);
	}

	@Test
	void aManualJobWaitsWhileItsTypeIsRunning() {
		givenRunning(TestBatch.runningJob(FIRST_JOB_ID, TestBatch.AT_10_00));
		BatchJob waiting = TestBatch.manualJob(SECOND_JOB_ID, TestBatch.AT_10_02);
		givenDue(waiting);

		assertThat(plannerAt(JUST_AFTER_10_02).startDueJobs()).isEmpty();

		assertThat(waiting.getStatus()).isEqualTo(BatchJobStatus.SCHEDULED);
		verify(jobs, never()).delete(any(BatchJob.class));
	}

	@Test
	void twoManualJobsOfOneTypeRunOneAfterTheOther() {
		BatchJob first = TestBatch.manualJob(FIRST_JOB_ID, TestBatch.AT_10_00);
		BatchJob second = TestBatch.manualJob(SECOND_JOB_ID, TestBatch.AT_10_02);
		givenDue(first, second);

		assertThat(plannerAt(JUST_AFTER_10_02).startDueJobs()).containsExactly(FIRST_JOB_ID);

		assertThat(second.getStatus()).isEqualTo(BatchJobStatus.SCHEDULED);
		verify(jobs, never()).delete(any(BatchJob.class));
		verify(jobs, never()).save(any(BatchJob.class));
	}

	@Test
	void aCronJobMissedDuringDowntimeRunsOnceAndTheChainContinuesFromNow() {
		givenDue(TestBatch.cronJob(FIRST_JOB_ID, TestBatch.AT_10_00));

		assertThat(plannerAt(AFTER_DOWNTIME).startDueJobs()).containsExactly(FIRST_JOB_ID);

		assertPlannedCronJobAt(NEXT_SLOT_AFTER_DOWNTIME);
	}

	@Test
	void aCronJobDueInTheQuietWindowWaitsAndRunsOnceWhenItEnds() {
		BatchJob due = TestBatch.cronJob(FIRST_JOB_ID, QUIET_MIDNIGHT);
		givenDue(due);

		assertThat(plannerAt(HALF_PAST_MIDNIGHT).startDueJobs()).isEmpty();
		assertThat(due.getStatus()).isEqualTo(BatchJobStatus.SCHEDULED);
		verify(jobs, never()).delete(any(BatchJob.class));
		verify(jobs, never()).save(any(BatchJob.class));

		assertThat(plannerAt(JUST_AFTER_THE_QUIET_WINDOW).startDueJobs()).containsExactly(FIRST_JOB_ID);
		assertPlannedCronJobAt(THREE_AT_NIGHT);
	}

	@Test
	void aManualJobWaitsForTheEndOfTheQuietWindow() {
		BatchJob waiting = TestBatch.manualJob(FIRST_JOB_ID, QUIET_MIDNIGHT);
		givenDue(waiting);

		assertThat(plannerAt(HALF_PAST_MIDNIGHT).startDueJobs()).isEmpty();

		assertThat(waiting.getStatus()).isEqualTo(BatchJobStatus.SCHEDULED);
	}

	@Test
	void aCronJobDueInTheQuietWindowWhileItsTypeStillRunsIsDeletedAsAnyOverrun() {
		givenRunning(TestBatch.runningJob(FIRST_JOB_ID, TEN_TO_MIDNIGHT));
		BatchJob overrun = TestBatch.cronJob(SECOND_JOB_ID, QUIET_MIDNIGHT);
		givenDue(overrun);

		assertThat(plannerAt(QUIET_MIDNIGHT).startDueJobs()).isEmpty();

		verify(jobs).delete(overrun);
		assertPlannedCronJobAt(ONE_AT_NIGHT);
	}

	@Test
	void recoveryFailsTheJobsLeftRunningByACrash() {
		BatchJob crashed = TestBatch.runningJob(FIRST_JOB_ID, TestBatch.AT_10_00);
		givenRunning(crashed);
		when(items.countsOf(FIRST_JOB_ID)).thenReturn(new BatchItemCounts(SUCCEEDED_BEFORE_CRASH, FAILED_BEFORE_CRASH));
		givenPlannedCronJob(true);

		plannerAt(AFTER_DOWNTIME).recover();

		assertThat(crashed.getStatus()).isEqualTo(BatchJobStatus.FAILED);
		assertThat(crashed.getErrorCode()).isEqualTo(BatchError.JOB_INTERRUPTED.code());
		assertThat(crashed.getSucceededItems()).isEqualTo(SUCCEEDED_BEFORE_CRASH);
		assertThat(crashed.getFailedItems()).isEqualTo(FAILED_BEFORE_CRASH);
		assertThat(crashed.getFinishedAt()).isEqualTo(AFTER_DOWNTIME);
		verify(jobs, never()).save(any(BatchJob.class));
	}

	@Test
	void recoveryPlansTheFirstJobOfATypeThatNeverRanForNow() {
		givenPlannedCronJob(false);
		when(jobs.existsByBatchTypeId(TestBatch.TYPE_ID)).thenReturn(false);

		plannerAt(HALF_PAST_10).recover();

		assertPlannedCronJobAt(HALF_PAST_10);
	}

	@Test
	void recoveryPlansTheNextSlotForATypeThatRanBefore() {
		givenPlannedCronJob(false);
		when(jobs.existsByBatchTypeId(TestBatch.TYPE_ID)).thenReturn(true);

		plannerAt(HALF_PAST_10).recover();

		assertPlannedCronJobAt(TestBatch.AT_11_00);
	}

	@Test
	void removingAPlannedCronJobPlansTheSlotAfterIt() {
		BatchJob planned = TestBatch.cronJob(FIRST_JOB_ID, TestBatch.AT_12_00);
		when(jobs.findById(FIRST_JOB_ID)).thenReturn(Optional.of(planned));

		plannerAt(HALF_PAST_10).remove(FIRST_JOB_ID);

		verify(historyActor).declareCurrentAuditor();
		verify(jobs).delete(planned);
		assertPlannedCronJobAt(TestBatch.AT_13_00);
	}

	@Test
	void removingAPlannedManualJobPlansNothingInItsPlace() {
		BatchJob planned = TestBatch.manualJob(FIRST_JOB_ID, TestBatch.AT_12_00);
		when(jobs.findById(FIRST_JOB_ID)).thenReturn(Optional.of(planned));

		plannerAt(HALF_PAST_10).remove(FIRST_JOB_ID);

		verify(jobs).delete(planned);
		verify(jobs, never()).save(any(BatchJob.class));
	}

	@Test
	void onlyAScheduledJobCanBeRemoved() {
		when(jobs.findById(FIRST_JOB_ID)).thenReturn(Optional.of(TestBatch.runningJob(FIRST_JOB_ID, TestBatch.AT_10_00)));

		assertThatThrownBy(() -> plannerAt(HALF_PAST_10).remove(FIRST_JOB_ID)).isInstanceOf(AppException.class)
				.extracting(failure -> ((AppException) failure).errorCode()).isEqualTo(BatchError.JOB_NOT_SCHEDULED);
		verify(jobs, never()).delete(any(BatchJob.class));
	}

	private BatchPlanner plannerAt(Instant now) {
		return new BatchPlannerImpl(types, jobs, items, historyActor, TestBatch.properties(), Clock.fixed(now, ZoneOffset.UTC));
	}

	private void givenDue(BatchJob... due) {
		when(jobs.findByStatusAndScheduledForLessThanEqualOrderByScheduledForAscIdAsc(any(BatchJobStatus.class), any(Instant.class))).thenReturn(List.of(due));
	}

	private void givenRunning(BatchJob running) {
		when(jobs.findByStatus(BatchJobStatus.RUNNING)).thenReturn(List.of(running));
	}

	private void givenPlannedCronJob(boolean planned) {
		when(jobs.existsByBatchTypeIdAndStatusAndOrigin(TestBatch.TYPE_ID, BatchJobStatus.SCHEDULED, BatchJobOrigin.CRON)).thenReturn(planned);
	}

	private void assertPlannedCronJobAt(Instant scheduledFor) {
		ArgumentCaptor<BatchJob> planned = ArgumentCaptor.forClass(BatchJob.class);
		verify(jobs).save(planned.capture());
		assertThat(planned.getValue().getOrigin()).isEqualTo(BatchJobOrigin.CRON);
		assertThat(planned.getValue().getStatus()).isEqualTo(BatchJobStatus.SCHEDULED);
		assertThat(planned.getValue().getScheduledFor()).isEqualTo(scheduledFor);
	}
}
