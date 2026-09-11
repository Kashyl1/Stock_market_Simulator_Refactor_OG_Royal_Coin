package com.tradingsimulator.processengine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tools.jackson.databind.json.JsonMapper;

class ProcessRewindTest {

	private static final String COUNTER = "COUNTER";
	private static final String COUNT_FIELD = "count";
	private static final int BUMPED_ONCE = 1;
	private static final String ANY_INPUT = "go";

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
		ProcessDefinition<CountContext> def = ProcessDefinition.builder(COUNTER, CountContext.class)
				.start(Step.BUMP)
				.step(Step.BUMP).on(StandardOutcome.CONTINUE).goTo(Step.HOLD)
				.step(Step.HOLD).on(StandardOutcome.CONTINUE).goTo(Step.DONE)
				.step(Step.DONE).end()
				.build();

		List<StepListener<?>> listeners = List.of(
				new StepListener<CountContext>() {
					public String processKey() {
						return COUNTER;
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
						return COUNTER;
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
		ProcessContextCodec codec = new ProcessContextCodec(JsonMapper.builder().build());
		engine = new ProcessEngine(registry, store, codec, new StepRunner(registry, store, codec),
				ProcessEngine.DEFAULT_MAX_TRANSITIONS_PER_RUN);
	}

	@Test
	void rewindRestoresTheSnapshotSoTheStepDoesNotStack() {
		ProcessInstanceView started = engine.start(COUNTER, new CountContext());
		assertThat(started.status()).isEqualTo(ProcessStatus.WAITING);
		assertThat(started.currentStep()).isEqualTo(Step.HOLD.name());
		assertThat(started.context().get(COUNT_FIELD).intValue()).isEqualTo(BUMPED_ONCE);

		ProcessInstanceView rewound = engine.rewind(started.id(), Step.BUMP.name());
		assertThat(rewound.status()).isEqualTo(ProcessStatus.WAITING);
		assertThat(rewound.currentStep()).isEqualTo(Step.HOLD.name());
		assertThat(rewound.context().get(COUNT_FIELD).intValue()).isEqualTo(BUMPED_ONCE);

		assertThat(engine.log(started.id())).anySatisfy(
				row -> assertThat(row.disposition()).isEqualTo(StepRunner.REWIND_DISPOSITION));
	}

	@Test
	void rewindToAStepThatNeverRanIsRejected() {
		UUID id = engine.start(COUNTER, new CountContext()).id();

		assertThatThrownBy(() -> engine.rewind(id, Step.DONE.name()))
				.isInstanceOf(ProcessExecutionException.class)
				.hasMessageContaining("nothing to rewind to");
	}

	@Test
	void aCompletedInstanceCannotBeRewound() {
		UUID id = engine.start(COUNTER, new CountContext()).id();
		ProcessInstanceView done = engine.signal(id, ANY_INPUT);
		assertThat(done.status()).isEqualTo(ProcessStatus.COMPLETED);

		assertThatThrownBy(() -> engine.rewind(id, Step.BUMP.name()))
				.isInstanceOf(ProcessClosedException.class)
				.hasMessageContaining(ProcessStatus.COMPLETED.name());
	}
}
