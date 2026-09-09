package com.tradingsimulator.processengine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProcessRewindTest {

	private ProcessEngine engine;

	enum Step implements StepKey { BUMP, HOLD, DONE }

	static final class CountContext extends ProcessContext {
		private int count;

		public int getCount() {
			return count;
		}

		public void setCount(int count) {
			this.count = count;
		}
	}

	@BeforeEach
	void setUp() {
		ProcessDefinition<CountContext> def = ProcessDefinition.builder("COUNTER", CountContext.class)
				.start(Step.BUMP)
				.step(Step.BUMP).on(StandardOutcome.CONTINUE).goTo(Step.HOLD)
				.step(Step.HOLD).on(StandardOutcome.CONTINUE).goTo(Step.DONE)
				.step(Step.DONE).end()
				.build();

		List<StepListener<?>> listeners = List.of(
				new StepListener<CountContext>() {
					public String processKey() {
						return "COUNTER";
					}

					public StepKey step() {
						return Step.BUMP;
					}

					public StepResult execute(StepExecution<CountContext> exec) {
						exec.context().setCount(exec.context().getCount() + 1);
						return StepResult.proceed();
					}
				},
				new StepListener<CountContext>() {
					public String processKey() {
						return "COUNTER";
					}

					public StepKey step() {
						return Step.HOLD;
					}

					public StepResult execute(StepExecution<CountContext> exec) {
						return exec.resumedByInput() ? StepResult.proceed() : StepResult.awaitInput();
					}
				});

		ProcessRegistry registry = new ProcessRegistry(List.of(def), listeners);
		registry.validate();
		InMemoryProcessInstanceStore store = new InMemoryProcessInstanceStore();
		ProcessContextCodec codec = new ProcessContextCodec();
		engine = new ProcessEngine(registry, store, codec, new StepRunner(registry, store, codec), 100);
	}

	@Test
	void rewindRestoresTheSnapshotSoTheStepDoesNotStack() {
		ProcessInstanceView started = engine.start("COUNTER", new CountContext());
		assertThat(started.status()).isEqualTo(ProcessStatus.WAITING);
		assertThat(started.currentStep()).isEqualTo("HOLD");
		assertThat(started.context().get("count").intValue()).isEqualTo(1);

		ProcessInstanceView rewound = engine.rewind(started.id(), "BUMP");
		assertThat(rewound.status()).isEqualTo(ProcessStatus.WAITING);
		assertThat(rewound.currentStep()).isEqualTo("HOLD");
		assertThat(rewound.context().get("count").intValue()).isEqualTo(1);

		assertThat(engine.log(started.id())).anySatisfy(
				row -> assertThat(row.disposition()).isEqualTo("REWIND"));
	}

	@Test
	void rewindToAStepThatNeverRanIsRejected() {
		UUID id = engine.start("COUNTER", new CountContext()).id();

		assertThatThrownBy(() -> engine.rewind(id, "DONE"))
				.isInstanceOf(ProcessExecutionException.class)
				.hasMessageContaining("nothing to rewind to");
	}

	@Test
	void aCompletedInstanceCannotBeRewound() {
		UUID id = engine.start("COUNTER", new CountContext()).id();
		ProcessInstanceView done = engine.signal(id, "go");
		assertThat(done.status()).isEqualTo(ProcessStatus.COMPLETED);

		assertThatThrownBy(() -> engine.rewind(id, "BUMP"))
				.isInstanceOf(ProcessClosedException.class)
				.hasMessageContaining("COMPLETED");
	}
}
