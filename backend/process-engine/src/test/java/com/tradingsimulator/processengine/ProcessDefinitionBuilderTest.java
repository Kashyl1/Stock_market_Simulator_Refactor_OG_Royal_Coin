package com.tradingsimulator.processengine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ProcessDefinitionBuilderTest {

	private static final String PROCESS_KEY = "P";

	enum S implements StepKey { A, B, C, D }

	enum O implements Outcome { GO, ALT }

	@Test
	void buildsValidLinearProcess() {
		ProcessDefinition<Ctx> def = ProcessDefinition.builder(PROCESS_KEY, Ctx.class)
				.start(S.A)
				.step(S.A).on(O.GO).goTo(S.B)
				.step(S.B).end()
				.build();

		assertThat(def.startStep()).isEqualTo(S.A);
		assertThat(def.isEndStep(S.B)).isTrue();
		assertThat(def.next(S.A, O.GO)).contains(S.B);
		assertThat(def.steps()).containsExactlyInAnyOrder(S.A, S.B);
	}

	@Test
	void branchingStepKeepsBothTransitions() {
		ProcessDefinition<Ctx> def = ProcessDefinition.builder(PROCESS_KEY, Ctx.class)
				.start(S.A)
				.step(S.A).on(O.GO).goTo(S.B)
				.step(S.A).on(O.ALT).goTo(S.C)
				.step(S.B).end()
				.step(S.C).end()
				.build();

		assertThat(def.next(S.A, O.GO)).contains(S.B);
		assertThat(def.next(S.A, O.ALT)).contains(S.C);
		assertThat(def.outcomesOf(S.A)).containsExactlyInAnyOrder(O.GO, O.ALT);
	}

	@Test
	void missingStartStepFails() {
		assertThatThrownBy(() -> ProcessDefinition.builder(PROCESS_KEY, Ctx.class)
				.step(S.A).end()
				.build())
				.isInstanceOf(ProcessDefinitionException.class)
				.hasMessageContaining("start step not set");
	}

	@Test
	void nonEndStepWithoutOutgoingFails() {
		assertThatThrownBy(() -> ProcessDefinition.builder(PROCESS_KEY, Ctx.class)
				.start(S.A)
				.build())
				.isInstanceOf(ProcessDefinitionException.class)
				.hasMessageContaining("no outgoing transitions");
	}

	@Test
	void endStepWithOutgoingFails() {
		assertThatThrownBy(() -> ProcessDefinition.builder(PROCESS_KEY, Ctx.class)
				.start(S.A)
				.step(S.A).on(O.GO).goTo(S.B)
				.step(S.B).end()
				.step(S.B).on(O.GO).goTo(S.A)
				.build())
				.isInstanceOf(ProcessDefinitionException.class)
				.hasMessageContaining("end step");
	}

	@Test
	void unreachableStepFails() {
		assertThatThrownBy(() -> ProcessDefinition.builder(PROCESS_KEY, Ctx.class)
				.start(S.A)
				.step(S.A).on(O.GO).goTo(S.B)
				.step(S.B).end()
				.step(S.C).on(O.GO).goTo(S.D)
				.step(S.D).end()
				.build())
				.isInstanceOf(ProcessDefinitionException.class)
				.hasMessageContaining("unreachable");
	}

	@Test
	void duplicateOutcomeOnSameStepFails() {
		assertThatThrownBy(() -> ProcessDefinition.builder(PROCESS_KEY, Ctx.class)
				.start(S.A)
				.step(S.A).on(O.GO).goTo(S.B)
				.step(S.A).on(O.GO).goTo(S.C)
				.build())
				.isInstanceOf(ProcessDefinitionException.class)
				.hasMessageContaining("already has a transition");
	}

	static final class Ctx extends ProcessContext {
	}
}
