package com.tradingsimulator.processengine;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionInterceptor;

import tools.jackson.databind.json.JsonMapper;

class ProcessStepFailureTest {

	private static final String PROCESS_KEY = "FAILING";
	private static final String WORK_FAILURE = "work failed";
	private static final String PARTIAL_NOTE = "half done";
	private static final String NOTE_FIELD = "note";
	private static final int ONE_TRANSACTION = 1;

	enum Step implements StepKey { WORK, DONE }

	static final class WorkContext extends ProcessContext {
		private String note;

		public String getNote() {
			return note;
		}

		public void setNote(String note) {
			this.note = note;
		}
	}

	public static class FailingWork {

		@Transactional
		public void perform() {
			throw new IllegalStateException(WORK_FAILURE);
		}
	}

	private final RecordingTransactionManager transactions = new RecordingTransactionManager();
	private ProcessEngine engine;

	@BeforeEach
	void setUp() {
		FailingWork work = transactional(new FailingWork());
		ProcessDefinition<WorkContext> definition = ProcessDefinition.builder(PROCESS_KEY, WorkContext.class)
				.start(Step.WORK)
				.step(Step.WORK).on(StandardOutcome.CONTINUE).goTo(Step.DONE)
				.step(Step.DONE).end()
				.build();
		StepListener<WorkContext> listener = new StepListener<>() {
			public String processKey() {
				return PROCESS_KEY;
			}

			public StepKey step() {
				return Step.WORK;
			}

			public StepResult execute(StepExecution<WorkContext> exec) {
				exec.context().setNote(PARTIAL_NOTE);
				work.perform();
				return StepResult.proceed();
			}
		};

		ProcessRegistry registry = new ProcessRegistry(List.of(definition), List.of(listener));
		registry.validate();
		InMemoryProcessInstanceStore store = new InMemoryProcessInstanceStore();
		ProcessContextCodec codec = new ProcessContextCodec(JsonMapper.builder().build());
		StepRunner runner = transactional(new StepRunner(registry, store, codec));
		engine = new ProcessEngine(registry, store, codec, runner, ProcessEngine.DEFAULT_MAX_TRANSITIONS_PER_RUN);
	}

	@Test
	void aListenerFailingInsideATransactionalServiceStillFailsTheInstance() {
		ProcessInstanceView view = engine.start(PROCESS_KEY, new WorkContext());

		assertThat(view.status()).isEqualTo(ProcessStatus.FAILED);
		assertThat(view.currentStep()).isEqualTo(Step.WORK.name());
		assertThat(view.context().get(NOTE_FIELD).isNull()).isTrue();
		assertThat(engine.log(view.id())).singleElement().satisfies(row -> {
			assertThat(row.disposition()).isEqualTo(StepResult.Disposition.FAILED.name());
			assertThat(row.error()).contains(WORK_FAILURE);
		});
		assertThat(transactions.rolledBack()).isEqualTo(ONE_TRANSACTION);
		assertThat(transactions.committed()).isEqualTo(ONE_TRANSACTION);
	}

	@SuppressWarnings("unchecked")
	private <T> T transactional(T target) {
		ProxyFactory factory = new ProxyFactory(target);
		factory.setProxyTargetClass(true);
		factory.addAdvice(new TransactionInterceptor(transactions, new AnnotationTransactionAttributeSource()));
		return (T) factory.getProxy();
	}
}
