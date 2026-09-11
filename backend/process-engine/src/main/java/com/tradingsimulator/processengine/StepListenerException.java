package com.tradingsimulator.processengine;

import java.time.Instant;

final class StepListenerException extends RuntimeException {

	private final Instant startedAt;

	StepListenerException(Instant startedAt, RuntimeException cause) {
		super(cause);
		this.startedAt = startedAt;
	}

	Instant startedAt() {
		return startedAt;
	}
}
