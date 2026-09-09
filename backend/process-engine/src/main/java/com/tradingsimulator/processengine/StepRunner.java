package com.tradingsimulator.processengine;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.tradingsimulator.processengine.store.ProcessInstanceStore;
import com.tradingsimulator.processengine.store.ProcessRecord;
import com.tradingsimulator.processengine.store.StepLogAppend;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class StepRunner {

	private final ProcessRegistry registry;
	private final ProcessInstanceStore store;
	private final ProcessContextCodec codec;

	public record StepProgress(ProcessStatus status, String nextStep) {
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public StepProgress runOne(UUID instanceId, Object input) {
		ProcessRecord record = store.find(instanceId)
				.orElseThrow(() -> new ProcessInstanceNotFoundException(instanceId));
		ProcessDefinition<?> def = registry.definition(record.definitionKey());
		StepKey step = resolveStep(def, record.currentStep());
		String contextBefore = record.contextJson();
		ProcessContext context = codec.decode(contextBefore, def.contextType());

		Instant startedAt = Instant.now();
		StepResult result;
		try {
			result = invokeListener(def, step, instanceId, context, input);
		}
		catch (RuntimeException ex) {
			log.error("Step '{}' of process '{}' (instance {}) threw", step.name(), def.key(), instanceId, ex);
			record.status(ProcessStatus.FAILED);
			record.updatedAt(Instant.now());
			store.save(record);
			appendLog(record, step, null, StepResult.Disposition.FAILED, startedAt, ex.toString(), contextBefore);
			return new StepProgress(ProcessStatus.FAILED, null);
		}

		record.contextJson(codec.encode(context));
		record.updatedAt(Instant.now());

		return switch (result.disposition()) {
			case AWAIT_INPUT -> {
				record.status(ProcessStatus.WAITING);
				store.save(record);
				appendLog(record, step, null, StepResult.Disposition.AWAIT_INPUT, startedAt, null, contextBefore);
				yield new StepProgress(ProcessStatus.WAITING, null);
			}
			case FAILED -> {
				record.status(ProcessStatus.FAILED);
				store.save(record);
				appendLog(record, step, null, StepResult.Disposition.FAILED, startedAt,
						result.error().orElse(null), contextBefore);
				yield new StepProgress(ProcessStatus.FAILED, null);
			}
			case ADVANCE -> advance(def, step, record, result.outcome().orElseThrow(), startedAt, contextBefore);
		};
	}

	private StepProgress advance(ProcessDefinition<?> def, StepKey step, ProcessRecord record, Outcome outcome,
			Instant startedAt, String contextBefore) {
		if (def.isEndStep(step)) {
			record.status(ProcessStatus.COMPLETED);
			store.save(record);
			appendLog(record, step, outcome.name(), StepResult.Disposition.ADVANCE, startedAt, null, contextBefore);
			return new StepProgress(ProcessStatus.COMPLETED, null);
		}

		Optional<StepKey> next = def.next(step, outcome);
		if (next.isEmpty()) {
			log.error("Process '{}': no transition from step '{}' for outcome '{}' (instance {})",
					def.key(), step.name(), outcome.name(), record.id());
			record.status(ProcessStatus.FAILED);
			store.save(record);
			appendLog(record, step, outcome.name(), StepResult.Disposition.FAILED, startedAt,
					"no transition for outcome '" + outcome.name() + "'", contextBefore);
			return new StepProgress(ProcessStatus.FAILED, null);
		}

		record.currentStep(next.get().name());
		record.status(ProcessStatus.RUNNING);
		store.save(record);
		appendLog(record, step, outcome.name(), StepResult.Disposition.ADVANCE, startedAt, null, contextBefore);
		return new StepProgress(ProcessStatus.RUNNING, next.get().name());
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private StepResult invokeListener(ProcessDefinition<?> def, StepKey step, UUID instanceId, ProcessContext context,
			Object input) {
		Optional<StepListener<?>> listener = registry.listener(def.key(), step);
		if (listener.isEmpty()) {
			return StepResult.proceed();
		}
		StepExecution execution = new StepExecution(instanceId, context, input);
		return ((StepListener) listener.get()).execute(execution);
	}

	private static StepKey resolveStep(ProcessDefinition<?> def, String stepName) {
		return def.steps().stream()
				.filter(s -> s.name().equals(stepName))
				.findFirst()
				.orElseThrow(() -> new ProcessExecutionException("process '" + def.key()
						+ "' has no step named '" + stepName + "'"));
	}

	private void appendLog(ProcessRecord record, StepKey step, String outcome, StepResult.Disposition disposition,
			Instant startedAt, String error, String contextBefore) {
		int sequenceNo = store.stepCount(record.id());
		store.appendLog(new StepLogAppend(record.id(), sequenceNo, step.name(), outcome, disposition.name(),
				startedAt, Instant.now(), error, contextBefore));
	}
}
