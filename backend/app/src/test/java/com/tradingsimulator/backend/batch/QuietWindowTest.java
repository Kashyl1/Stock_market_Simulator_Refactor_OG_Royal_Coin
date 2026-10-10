package com.tradingsimulator.backend.batch;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

class QuietWindowTest {

	private static final ZoneId WARSAW = ZoneId.of("Europe/Warsaw");
	private static final LocalTime MIDNIGHT = LocalTime.MIDNIGHT;
	private static final LocalTime HALF_PAST_TWO = LocalTime.of(2, 30);
	private static final LocalTime ELEVEN_PM = LocalTime.of(23, 0);
	private static final LocalTime ONE_AM = LocalTime.of(1, 0);
	private static final BatchProperties.QuietWindow NIGHTLY = new BatchProperties.QuietWindow(MIDNIGHT, HALF_PAST_TWO, WARSAW);
	private static final BatchProperties.QuietWindow ACROSS_MIDNIGHT = new BatchProperties.QuietWindow(ELEVEN_PM, ONE_AM, WARSAW);
	private static final Instant MIDNIGHT_IN_WARSAW = Instant.parse("2026-10-09T22:00:00Z");
	private static final Instant HALF_PAST_ONE_IN_WARSAW = Instant.parse("2026-10-09T23:30:00Z");
	private static final Instant HALF_PAST_TWO_IN_WARSAW = Instant.parse("2026-10-10T00:30:00Z");
	private static final Instant MORNING_IN_WARSAW = Instant.parse("2026-10-10T08:00:00Z");
	private static final Instant HALF_PAST_ELEVEN_PM_IN_WARSAW = Instant.parse("2026-10-09T21:30:00Z");
	private static final Duration POLL_INTERVAL = Duration.ofSeconds(15);
	private static final Duration SHUTDOWN_TIMEOUT = Duration.ofSeconds(30);

	@Test
	void coversTheNightInItsOwnZone() {
		assertThat(NIGHTLY.contains(HALF_PAST_ONE_IN_WARSAW)).isTrue();
		assertThat(NIGHTLY.contains(MORNING_IN_WARSAW)).isFalse();
	}

	@Test
	void startsAtItsStartAndEndsJustBeforeItsEnd() {
		assertThat(NIGHTLY.contains(MIDNIGHT_IN_WARSAW)).isTrue();
		assertThat(NIGHTLY.contains(HALF_PAST_TWO_IN_WARSAW)).isFalse();
	}

	@Test
	void mayCrossMidnight() {
		assertThat(ACROSS_MIDNIGHT.contains(HALF_PAST_ELEVEN_PM_IN_WARSAW)).isTrue();
		assertThat(ACROSS_MIDNIGHT.contains(MIDNIGHT_IN_WARSAW)).isTrue();
		assertThat(ACROSS_MIDNIGHT.contains(HALF_PAST_ONE_IN_WARSAW)).isFalse();
	}

	@Test
	void aWindowEndingWhereItStartsIsEmpty() {
		assertThat(new BatchProperties.QuietWindow(MIDNIGHT, MIDNIGHT, WARSAW).contains(MIDNIGHT_IN_WARSAW)).isFalse();
	}

	@Test
	void anEngineWithoutAWindowIsNeverQuiet() {
		assertThat(new BatchProperties(true, POLL_INTERVAL, SHUTDOWN_TIMEOUT, null).quietAt(HALF_PAST_ONE_IN_WARSAW)).isFalse();
	}
}
