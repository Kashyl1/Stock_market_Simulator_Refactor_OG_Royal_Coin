package com.tradingsimulator.processengine.store;

import java.time.Instant;
import java.util.UUID;

public record StepLogAppend(
		UUID instanceId,
		int sequenceNo,
		String step,
		String outcome,
		String disposition,
		Instant startedAt,
		Instant finishedAt,
		String error,
		String contextBefore) {
}
