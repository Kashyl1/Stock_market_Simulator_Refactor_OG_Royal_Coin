package com.tradingsimulator.processengine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tools.jackson.databind.json.JsonMapper;

class ProcessEngineTest {

	private static final String ORDER = "ORDER";
	private static final String LOOP = "LOOP";
	private static final String UNKNOWN_PROCESS = "NOPE";
	private static final String RESULT_FIELD = "result";
	private static final String LOG_SEPARATOR = ":";
	private static final double LARGE_ORDER_THRESHOLD = 1000;
	private static final double SMALL_AMOUNT = 100;
	private static final double LARGE_AMOUNT = 5000;
	private static final double ANY_AMOUNT = 1;
	private static final int LOOP_GUARD = 5;

	private InMemoryProcessInstanceStore store;
	private ProcessEngine engine;

	enum OrderStep implements StepKey { VALIDATE, REVIEW, EXECUTE, REJECTED }

	enum OrderOutcome implements Outcome { SMALL, LARGE, APPROVED, DECLINED }

	enum OrderResult { EXECUTED, REJECTED }

	static final class OrderContext extends ProcessContext {
		private double amount;
		private OrderResult result;

		public OrderContext() {
		}

		OrderContext(double amount) {
			this.amount = amount;
		}

		public double getAmount() {
			return amount;
		}

		public void setAmount(double amount) {
			this.amount = amount;
		}

		public OrderResult getResult() {
			return result;
		}

		public void setResult(OrderResult result) {
			this.result = result;
		}
	}

	@BeforeEach
	void setUp() {
		ProcessDefinition<OrderContext> orderProcess = ProcessDefinition.builder(ORDER, OrderContext.class)
				.start(OrderStep.VALIDATE)
				.step(OrderStep.VALIDATE).on(OrderOutcome.SMALL).goTo(OrderStep.EXECUTE)
				.step(OrderStep.VALIDATE).on(OrderOutcome.LARGE).goTo(OrderStep.REVIEW)
				.step(OrderStep.REVIEW).on(OrderOutcome.APPROVED).goTo(OrderStep.EXECUTE)
				.step(OrderStep.REVIEW).on(OrderOutcome.DECLINED).goTo(OrderStep.REJECTED)
				.step(OrderStep.EXECUTE).end()
				.step(OrderStep.REJECTED).end()
				.build();

		List<StepListener<?>> listeners = List.of(
				listener(ORDER, OrderStep.VALIDATE, exec -> {
					OrderContext c = (OrderContext) exec.context();
					return StepResult.advance(
							c.getAmount() > LARGE_ORDER_THRESHOLD ? OrderOutcome.LARGE : OrderOutcome.SMALL);
				}),
				listener(ORDER, OrderStep.REVIEW, exec -> {
					if (!exec.resumedByInput()) {
						return StepResult.awaitInput();
					}
					boolean approved = Boolean.TRUE.equals(exec.input().orElse(null));
					return StepResult.advance(approved ? OrderOutcome.APPROVED : OrderOutcome.DECLINED);
				}),
				listener(ORDER, OrderStep.EXECUTE, exec -> {
					((OrderContext) exec.context()).setResult(OrderResult.EXECUTED);
					return StepResult.proceed();
				}),
				listener(ORDER, OrderStep.REJECTED, exec -> {
					((OrderContext) exec.context()).setResult(OrderResult.REJECTED);
					return StepResult.proceed();
				}));

		ProcessRegistry registry = new ProcessRegistry(List.of(orderProcess), listeners);
		registry.validate();

		this.store = new InMemoryProcessInstanceStore();
		ProcessContextCodec codec = new ProcessContextCodec(JsonMapper.builder().build());
		StepRunner runner = new StepRunner(registry, store, codec);
		this.engine = new ProcessEngine(registry, store, codec, runner, ProcessEngine.DEFAULT_MAX_TRANSITIONS_PER_RUN);
	}

	@Test
	void smallOrderRunsStraightThroughToCompletion() {
		ProcessInstanceView view = engine.start(ORDER, new OrderContext(SMALL_AMOUNT));

		assertThat(view.status()).isEqualTo(ProcessStatus.COMPLETED);
		assertThat(view.currentStep()).isEqualTo(OrderStep.EXECUTE.name());
		assertThat(view.context().get(RESULT_FIELD).asString()).isEqualTo(OrderResult.EXECUTED.name());
		assertThat(store.log(view.id())).extracting(l -> l.step() + LOG_SEPARATOR + l.outcome())
				.containsExactly(
						transition(OrderStep.VALIDATE, OrderOutcome.SMALL),
						transition(OrderStep.EXECUTE, StandardOutcome.CONTINUE));
	}

	@Test
	void largeOrderParksForReviewThenResumesOnApproval() {
		ProcessInstanceView started = engine.start(ORDER, new OrderContext(LARGE_AMOUNT));
		assertThat(started.status()).isEqualTo(ProcessStatus.WAITING);
		assertThat(started.currentStep()).isEqualTo(OrderStep.REVIEW.name());

		ProcessInstanceView resumed = engine.signal(started.id(), Boolean.TRUE);
		assertThat(resumed.status()).isEqualTo(ProcessStatus.COMPLETED);
		assertThat(resumed.currentStep()).isEqualTo(OrderStep.EXECUTE.name());
		assertThat(resumed.context().get(RESULT_FIELD).asString()).isEqualTo(OrderResult.EXECUTED.name());
	}

	@Test
	void largeOrderCanBeDeclinedAtReview() {
		UUID id = engine.start(ORDER, new OrderContext(LARGE_AMOUNT)).id();

		ProcessInstanceView resumed = engine.signal(id, Boolean.FALSE);

		assertThat(resumed.status()).isEqualTo(ProcessStatus.COMPLETED);
		assertThat(resumed.currentStep()).isEqualTo(OrderStep.REJECTED.name());
		assertThat(resumed.context().get(RESULT_FIELD).asString()).isEqualTo(OrderResult.REJECTED.name());
	}

	@Test
	void signalOnAnInstanceThatIsNotWaitingIsRejected() {
		UUID id = engine.start(ORDER, new OrderContext(SMALL_AMOUNT)).id();

		assertThatThrownBy(() -> engine.signal(id, Boolean.TRUE))
				.isInstanceOf(IllegalProcessStateException.class);
	}

	@Test
	void unknownProcessKeyIsRejected() {
		assertThatThrownBy(() -> engine.start(UNKNOWN_PROCESS, new OrderContext(ANY_AMOUNT)))
				.isInstanceOf(UnknownProcessException.class);
	}

	@Test
	void cyclicDefinitionTripsTheLoopGuard() {
		ProcessDefinition<OrderContext> loop = ProcessDefinition.builder(LOOP, OrderContext.class)
				.start(OrderStep.VALIDATE)
				.step(OrderStep.VALIDATE).on(OrderOutcome.SMALL).goTo(OrderStep.VALIDATE)
				.build();
		ProcessRegistry registry = new ProcessRegistry(List.of(loop),
				List.of(listener(LOOP, OrderStep.VALIDATE, exec -> StepResult.advance(OrderOutcome.SMALL))));
		registry.validate();
		ProcessContextCodec codec = new ProcessContextCodec(JsonMapper.builder().build());
		InMemoryProcessInstanceStore loopStore = new InMemoryProcessInstanceStore();
		ProcessEngine loopEngine = new ProcessEngine(registry, loopStore, codec,
				new StepRunner(registry, loopStore, codec), LOOP_GUARD);

		assertThatThrownBy(() -> loopEngine.start(LOOP, new OrderContext(ANY_AMOUNT)))
				.isInstanceOf(ProcessExecutionException.class)
				.hasMessageContaining("exceeded " + LOOP_GUARD + " transitions");
	}

	private static String transition(StepKey step, Outcome outcome) {
		return step.name() + LOG_SEPARATOR + outcome.name();
	}

	private static <C extends ProcessContext> StepListener<C> listener(String processKey, StepKey step,
			Function<StepExecution<C>, StepResult> body) {
		return new StepListener<>() {
			@Override
			public String processKey() {
				return processKey;
			}

			@Override
			public StepKey step() {
				return step;
			}

			@Override
			public StepResult execute(StepExecution<C> execution) {
				return body.apply(execution);
			}
		};
	}
}
