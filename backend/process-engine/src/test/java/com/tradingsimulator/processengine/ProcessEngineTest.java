package com.tradingsimulator.processengine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProcessEngineTest {

	private InMemoryProcessInstanceStore store;
	private ProcessEngine engine;

	enum OrderStep implements StepKey { VALIDATE, REVIEW, EXECUTE, REJECTED }

	enum OrderOutcome implements Outcome { SMALL, LARGE, APPROVED, DECLINED }

	static final class OrderContext extends ProcessContext {
		private double amount;
		private String result;

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

		public String getResult() {
			return result;
		}

		public void setResult(String result) {
			this.result = result;
		}
	}

	@BeforeEach
	void setUp() {
		ProcessDefinition<OrderContext> orderProcess = ProcessDefinition.builder("ORDER", OrderContext.class)
				.start(OrderStep.VALIDATE)
				.step(OrderStep.VALIDATE).on(OrderOutcome.SMALL).goTo(OrderStep.EXECUTE)
				.step(OrderStep.VALIDATE).on(OrderOutcome.LARGE).goTo(OrderStep.REVIEW)
				.step(OrderStep.REVIEW).on(OrderOutcome.APPROVED).goTo(OrderStep.EXECUTE)
				.step(OrderStep.REVIEW).on(OrderOutcome.DECLINED).goTo(OrderStep.REJECTED)
				.step(OrderStep.EXECUTE).end()
				.step(OrderStep.REJECTED).end()
				.build();

		List<StepListener<?>> listeners = List.of(
				listener("ORDER", OrderStep.VALIDATE, exec -> {
					OrderContext c = (OrderContext) exec.context();
					return StepResult.advance(c.getAmount() > 1000 ? OrderOutcome.LARGE : OrderOutcome.SMALL);
				}),
				listener("ORDER", OrderStep.REVIEW, exec -> {
					if (!exec.resumedByInput()) {
						return StepResult.awaitInput();
					}
					boolean approved = Boolean.TRUE.equals(exec.input().orElse(null));
					return StepResult.advance(approved ? OrderOutcome.APPROVED : OrderOutcome.DECLINED);
				}),
				listener("ORDER", OrderStep.EXECUTE, exec -> {
					((OrderContext) exec.context()).setResult("EXECUTED");
					return StepResult.proceed();
				}),
				listener("ORDER", OrderStep.REJECTED, exec -> {
					((OrderContext) exec.context()).setResult("REJECTED");
					return StepResult.proceed();
				}));

		ProcessRegistry registry = new ProcessRegistry(List.of(orderProcess), listeners);
		registry.validate();

		this.store = new InMemoryProcessInstanceStore();
		ProcessContextCodec codec = new ProcessContextCodec();
		StepRunner runner = new StepRunner(registry, store, codec);
		this.engine = new ProcessEngine(registry, store, codec, runner, 100);
	}

	@Test
	void smallOrderRunsStraightThroughToCompletion() {
		ProcessInstanceView view = engine.start("ORDER", new OrderContext(100));

		assertThat(view.status()).isEqualTo(ProcessStatus.COMPLETED);
		assertThat(view.currentStep()).isEqualTo("EXECUTE");
		assertThat(view.context().get("result").asString()).isEqualTo("EXECUTED");
		assertThat(store.log(view.id())).extracting(l -> l.step() + ":" + l.outcome())
				.containsExactly("VALIDATE:SMALL", "EXECUTE:CONTINUE");
	}

	@Test
	void largeOrderParksForReviewThenResumesOnApproval() {
		ProcessInstanceView started = engine.start("ORDER", new OrderContext(5000));
		assertThat(started.status()).isEqualTo(ProcessStatus.WAITING);
		assertThat(started.currentStep()).isEqualTo("REVIEW");

		ProcessInstanceView resumed = engine.signal(started.id(), Boolean.TRUE);
		assertThat(resumed.status()).isEqualTo(ProcessStatus.COMPLETED);
		assertThat(resumed.currentStep()).isEqualTo("EXECUTE");
		assertThat(resumed.context().get("result").asString()).isEqualTo("EXECUTED");
	}

	@Test
	void largeOrderCanBeDeclinedAtReview() {
		UUID id = engine.start("ORDER", new OrderContext(5000)).id();

		ProcessInstanceView resumed = engine.signal(id, Boolean.FALSE);

		assertThat(resumed.status()).isEqualTo(ProcessStatus.COMPLETED);
		assertThat(resumed.currentStep()).isEqualTo("REJECTED");
		assertThat(resumed.context().get("result").asString()).isEqualTo("REJECTED");
	}

	@Test
	void signalOnAnInstanceThatIsNotWaitingIsRejected() {
		UUID id = engine.start("ORDER", new OrderContext(100)).id();

		assertThatThrownBy(() -> engine.signal(id, Boolean.TRUE))
				.isInstanceOf(IllegalProcessStateException.class);
	}

	@Test
	void unknownProcessKeyIsRejected() {
		assertThatThrownBy(() -> engine.start("NOPE", new OrderContext(1)))
				.isInstanceOf(UnknownProcessException.class);
	}

	@Test
	void cyclicDefinitionTripsTheLoopGuard() {
		ProcessDefinition<OrderContext> loop = ProcessDefinition.builder("LOOP", OrderContext.class)
				.start(OrderStep.VALIDATE)
				.step(OrderStep.VALIDATE).on(OrderOutcome.SMALL).goTo(OrderStep.VALIDATE)
				.build();
		ProcessRegistry registry = new ProcessRegistry(List.of(loop),
				List.of(listener("LOOP", OrderStep.VALIDATE, exec -> StepResult.advance(OrderOutcome.SMALL))));
		registry.validate();
		ProcessContextCodec codec = new ProcessContextCodec();
		InMemoryProcessInstanceStore loopStore = new InMemoryProcessInstanceStore();
		ProcessEngine loopEngine = new ProcessEngine(registry, loopStore, codec,
				new StepRunner(registry, loopStore, codec), 5);

		assertThatThrownBy(() -> loopEngine.start("LOOP", new OrderContext(1)))
				.isInstanceOf(ProcessExecutionException.class)
				.hasMessageContaining("exceeded 5 transitions");
	}

	private static <C extends ProcessContext> StepListener<C> listener(String processKey, StepKey step,
			java.util.function.Function<StepExecution<C>, StepResult> body) {
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
