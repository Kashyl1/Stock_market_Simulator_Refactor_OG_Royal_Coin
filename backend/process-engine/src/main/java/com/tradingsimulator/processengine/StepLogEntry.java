package com.tradingsimulator.processengine;

import java.time.Instant;

public record StepLogEntry(
		int sequenceNo,
		String step,
		String outcome,
		String disposition,
		Instant startedAt,
		Instant finishedAt,
		String error) {
}
