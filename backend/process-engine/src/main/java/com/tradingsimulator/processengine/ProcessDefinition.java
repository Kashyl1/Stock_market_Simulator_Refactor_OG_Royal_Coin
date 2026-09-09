package com.tradingsimulator.processengine;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class ProcessDefinition<C extends ProcessContext> {

	private final String key;
	private final Class<C> contextType;
	private final StepKey startStep;
	private final Map<StepKey, Map<Outcome, StepKey>> transitions;
	private final Set<StepKey> endSteps;
	private final Set<StepKey> allSteps;

	private ProcessDefinition(Builder<C> b) {
		this.key = b.key;
		this.contextType = b.contextType;
		this.startStep = b.startStep;
		this.endSteps = Set.copyOf(b.endSteps);
		Map<StepKey, Map<Outcome, StepKey>> copy = new LinkedHashMap<>();
		b.transitions.forEach((step, byOutcome) -> copy.put(step, Collections.unmodifiableMap(new LinkedHashMap<>(byOutcome))));
		this.transitions = Collections.unmodifiableMap(copy);
		this.allSteps = Set.copyOf(b.collectSteps());
	}

	public String key() {
		return key;
	}

	public Class<C> contextType() {
		return contextType;
	}

	public StepKey startStep() {
		return startStep;
	}

	public Set<StepKey> steps() {
		return allSteps;
	}

	public boolean isEndStep(StepKey step) {
		return endSteps.contains(step);
	}

	public Set<Outcome> outcomesOf(StepKey step) {
		return transitions.getOrDefault(step, Map.of()).keySet();
	}

	public Optional<StepKey> next(StepKey step, Outcome outcome) {
		return Optional.ofNullable(transitions.getOrDefault(step, Map.of()).get(outcome));
	}

	public static <C extends ProcessContext> Builder<C> builder(String key, Class<C> contextType) {
		return new Builder<>(key, contextType);
	}

	public static final class Builder<C extends ProcessContext> {

		private final String key;
		private final Class<C> contextType;
		private StepKey startStep;
		private final Map<StepKey, Map<Outcome, StepKey>> transitions = new LinkedHashMap<>();
		private final Set<StepKey> endSteps = new LinkedHashSet<>();

		private Builder(String key, Class<C> contextType) {
			if (key == null || key.isBlank()) {
				throw new ProcessDefinitionException("process key must not be blank");
			}
			if (contextType == null) {
				throw new ProcessDefinitionException("context type is required for process '" + key + "'");
			}
			this.key = key;
			this.contextType = contextType;
		}

		public Builder<C> start(StepKey step) {
			this.startStep = step;
			return this;
		}

		public StepConfigurer<C> step(StepKey step) {
			return new StepConfigurer<>(this, step);
		}

		public Builder<C> endStep(StepKey step) {
			endSteps.add(step);
			return this;
		}

		void addTransition(StepKey from, Outcome on, StepKey to) {
			Map<Outcome, StepKey> byOutcome = transitions.computeIfAbsent(from, k -> new LinkedHashMap<>());
			if (byOutcome.containsKey(on)) {
				throw new ProcessDefinitionException("process '" + key + "': step '" + from.name()
						+ "' already has a transition for outcome '" + on.name() + "'");
			}
			byOutcome.put(on, to);
		}

		Set<StepKey> collectSteps() {
			Set<StepKey> steps = new LinkedHashSet<>();
			if (startStep != null) {
				steps.add(startStep);
			}
			steps.addAll(endSteps);
			transitions.forEach((from, byOutcome) -> {
				steps.add(from);
				steps.addAll(byOutcome.values());
			});
			return steps;
		}

		public ProcessDefinition<C> build() {
			if (startStep == null) {
				throw new ProcessDefinitionException("process '" + key + "': start step not set");
			}
			Set<StepKey> steps = collectSteps();

			for (StepKey end : endSteps) {
				if (transitions.containsKey(end) && !transitions.get(end).isEmpty()) {
					throw new ProcessDefinitionException(
							"process '" + key + "': end step '" + end.name() + "' must have no outgoing transitions");
				}
			}
			for (StepKey step : steps) {
				boolean hasOutgoing = transitions.containsKey(step) && !transitions.get(step).isEmpty();
				if (!endSteps.contains(step) && !hasOutgoing) {
					throw new ProcessDefinitionException("process '" + key + "': step '" + step.name()
							+ "' is not an end step but has no outgoing transitions");
				}
			}

			Set<StepKey> reachable = new HashSet<>();
			Deque<StepKey> queue = new ArrayDeque<>();
			queue.add(startStep);
			reachable.add(startStep);
			while (!queue.isEmpty()) {
				StepKey current = queue.poll();
				for (StepKey target : transitions.getOrDefault(current, Map.of()).values()) {
					if (reachable.add(target)) {
						queue.add(target);
					}
				}
			}
			Set<StepKey> unreachable = new LinkedHashSet<>(steps);
			unreachable.removeAll(reachable);
			if (!unreachable.isEmpty()) {
				throw new ProcessDefinitionException("process '" + key + "': steps unreachable from start: "
						+ unreachable.stream().map(StepKey::name).toList());
			}

			return new ProcessDefinition<>(this);
		}
	}

	public static final class StepConfigurer<C extends ProcessContext> {

		private final Builder<C> builder;
		private final StepKey source;

		private StepConfigurer(Builder<C> builder, StepKey source) {
			this.builder = builder;
			this.source = source;
		}

		public OutcomeConfigurer<C> on(Outcome outcome) {
			return new OutcomeConfigurer<>(builder, source, outcome);
		}

		public Builder<C> end() {
			return builder.endStep(source);
		}
	}

	public static final class OutcomeConfigurer<C extends ProcessContext> {

		private final Builder<C> builder;
		private final StepKey source;
		private final Outcome outcome;

		private OutcomeConfigurer(Builder<C> builder, StepKey source, Outcome outcome) {
			this.builder = builder;
			this.source = source;
			this.outcome = outcome;
		}

		public Builder<C> goTo(StepKey target) {
			builder.addTransition(source, outcome, target);
			return builder;
		}
	}
}
