package com.tradingsimulator.processengine;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.tradingsimulator.processengine.store.ProcessInstanceStore;
import com.tradingsimulator.processengine.store.ProcessRecord;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class ProcessEngine {

	public static final int DEFAULT_MAX_TRANSITIONS_PER_RUN = 100;
	private static final String MAX_TRANSITIONS_PER_RUN =
			"${process-engine.max-transitions-per-run:" + DEFAULT_MAX_TRANSITIONS_PER_RUN + "}";

	private final ProcessRegistry registry;
	private final ProcessInstanceStore store;
	private final ProcessContextCodec codec;
	private final StepRunner stepRunner;
	private final int maxTransitionsPerRun;

	public ProcessEngine(ProcessRegistry registry, ProcessInstanceStore store, ProcessContextCodec codec,
			StepRunner stepRunner,
			@Value(MAX_TRANSITIONS_PER_RUN) int maxTransitionsPerRun) {
		this.registry = registry;
		this.store = store;
		this.codec = codec;
		this.stepRunner = stepRunner;
		this.maxTransitionsPerRun = maxTransitionsPerRun;
	}

	public ProcessInstanceView start(String definitionKey, ProcessContext initialContext) {
		ProcessDefinition<?> def = registry.definition(definitionKey);
		if (!def.contextType().isInstance(initialContext)) {
			throw new ProcessExecutionException("process '" + definitionKey + "' expects context "
					+ def.contextType().getName() + " but got " + initialContext.getClass().getName());
		}
		ProcessRecord record = store.create(definitionKey, def.startStep().name(), codec.encode(initialContext));
		log.info("Started process '{}' instance {}", definitionKey, record.id());
		return drive(record.id(), null);
	}

	public ProcessInstanceView signal(UUID instanceId, Object input) {
		ProcessRecord record = store.find(instanceId)
				.orElseThrow(() -> new ProcessInstanceNotFoundException(instanceId));
		if (record.status() != ProcessStatus.WAITING) {
			throw new IllegalProcessStateException(instanceId, record.status());
		}
		log.debug("Signalling process instance {} on step {}", instanceId, record.currentStep());
		return drive(instanceId, input);
	}

	public ProcessInstanceView view(UUID instanceId) {
		return toView(store.find(instanceId)
				.orElseThrow(() -> new ProcessInstanceNotFoundException(instanceId)));
	}

	public List<StepLogEntry> log(UUID instanceId) {
		store.find(instanceId).orElseThrow(() -> new ProcessInstanceNotFoundException(instanceId));
		return store.log(instanceId);
	}

	public ProcessInstanceView rewind(UUID instanceId, String stepName) {
		stepRunner.rewind(instanceId, stepName);
		log.info("Rewound process instance {} to step {}", instanceId, stepName);
		return drive(instanceId, null);
	}

	private ProcessInstanceView drive(UUID instanceId, Object firstInput) {
		Object input = firstInput;
		int transitions = 0;
		while (true) {
			StepRunner.StepProgress progress = runStep(instanceId, input);
			input = null;

			if (progress.status() != ProcessStatus.RUNNING) {
				return view(instanceId);
			}
			if (++transitions > maxTransitionsPerRun) {
				String reason = "process instance " + instanceId + " exceeded " + maxTransitionsPerRun
						+ " transitions in one run (likely a cycle in the definition)";
				stepRunner.recordFailure(instanceId, Instant.now(), reason);
				throw new ProcessExecutionException(reason);
			}
		}
	}

	private StepRunner.StepProgress runStep(UUID instanceId, Object input) {
		try {
			return stepRunner.runOne(instanceId, input);
		}
		catch (StepListenerException failure) {
			log.error("A step listener of process instance {} threw; the step was rolled back", instanceId,
					failure.getCause());
			stepRunner.recordFailure(instanceId, failure.startedAt(), failure.getCause().toString());
			return new StepRunner.StepProgress(ProcessStatus.FAILED, null);
		}
	}

	private ProcessInstanceView toView(ProcessRecord r) {
		return new ProcessInstanceView(r.id(), r.definitionKey(), r.currentStep(), r.status(),
				codec.toNode(r.contextJson()), r.createdAt(), r.updatedAt());
	}
}
