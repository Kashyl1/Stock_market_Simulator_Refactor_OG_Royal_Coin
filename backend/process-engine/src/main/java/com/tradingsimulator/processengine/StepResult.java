package com.tradingsimulator.processengine;

import java.util.Optional;

public final class StepResult {

	public enum Disposition {

		ADVANCE,

		AWAIT_INPUT,

		FAILED
	}

	private final Disposition disposition;
	private final Outcome outcome;
	private final String error;

	private StepResult(Disposition disposition, Outcome outcome, String error) {
		this.disposition = disposition;
		this.outcome = outcome;
		this.error = error;
	}

	public static StepResult advance(Outcome outcome) {
		if (outcome == null) {
			throw new IllegalArgumentException("outcome is required when advancing");
		}
		return new StepResult(Disposition.ADVANCE, outcome, null);
	}

	public static StepResult proceed() {
		return advance(StandardOutcome.CONTINUE);
	}

	public static StepResult awaitInput() {
		return new StepResult(Disposition.AWAIT_INPUT, null, null);
	}

	public static StepResult fail(String error) {
		return new StepResult(Disposition.FAILED, null, error);
	}

	public Disposition disposition() {
		return disposition;
	}

	public Optional<Outcome> outcome() {
		return Optional.ofNullable(outcome);
	}

	public Optional<String> error() {
		return Optional.ofNullable(error);
	}

	@Override
	public String toString() {
		return "StepResult[" + disposition + (outcome != null ? ", outcome=" + outcome.name() : "")
				+ (error != null ? ", error=" + error : "") + "]";
	}
}
