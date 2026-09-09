package com.tradingsimulator.processengine;

import java.util.Optional;
import java.util.UUID;

public final class StepExecution<C extends ProcessContext> {

	private final UUID instanceId;
	private final C context;
	private final Object input;

	public StepExecution(UUID instanceId, C context, Object input) {
		this.instanceId = instanceId;
		this.context = context;
		this.input = input;
	}

	public UUID instanceId() {
		return instanceId;
	}

	public C context() {
		return context;
	}

	public boolean resumedByInput() {
		return input != null;
	}

	public Optional<Object> input() {
		return Optional.ofNullable(input);
	}

	public <T> Optional<T> input(Class<T> type) {
		return type.isInstance(input) ? Optional.of(type.cast(input)) : Optional.empty();
	}
}
