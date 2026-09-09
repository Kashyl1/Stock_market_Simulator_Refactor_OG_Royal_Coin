package com.tradingsimulator.processengine;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.tradingsimulator.processengine.store.ProcessInstanceStore;
import com.tradingsimulator.processengine.store.ProcessRecord;
import com.tradingsimulator.processengine.store.StepLogAppend;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class ProcessEngine {

	private final ProcessRegistry registry;
	private final ProcessInstanceStore store;
	private final ProcessContextCodec codec;
	private final StepRunner stepRunner;
	private final int maxTransitionsPerRun;

	public ProcessEngine(ProcessRegistry registry, ProcessInstanceStore store, ProcessContextCodec codec,
			StepRunner stepRunner,
			@Value("${process-engine.max-transitions-per-run:100}") int maxTransitionsPerRun) {
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
		ProcessRecord record = store.find(instanceId)
				.orElseThrow(() -> new ProcessInstanceNotFoundException(instanceId));
		if (record.status() != ProcessStatus.RUNNING && record.status() != ProcessStatus.WAITING) {
			throw new ProcessClosedException(instanceId, record.status());
		}
		ProcessDefinition<?> def = registry.definition(record.definitionKey());
		boolean known = def.steps().stream().anyMatch(s -> s.name().equals(stepName));
		if (!known) {
			throw new ProcessExecutionException("process '" + def.key() + "' has no step named '" + stepName + "'");
		}
		String snapshot = store.latestContextBefore(instanceId, stepName)
				.orElseThrow(() -> new ProcessExecutionException("step '" + stepName + "' has not run on instance "
						+ instanceId + "; nothing to rewind to"));

		Instant now = Instant.now();
		store.appendLog(new StepLogAppend(instanceId, store.stepCount(instanceId), stepName, null, "REWIND",
				now, now, "rewound from " + record.currentStep(), record.contextJson()));

		record.contextJson(snapshot);
		record.currentStep(stepName);
		record.status(ProcessStatus.RUNNING);
		record.updatedAt(now);
		store.save(record);

		log.info("Rewound process instance {} to step {}", instanceId, stepName);
		return drive(instanceId, null);
	}

	private ProcessInstanceView drive(UUID instanceId, Object firstInput) {
		Object input = firstInput;
		int transitions = 0;
		while (true) {
			StepRunner.StepProgress progress = stepRunner.runOne(instanceId, input);
			input = null;

			if (progress.status() != ProcessStatus.RUNNING) {
				return view(instanceId);
			}
			if (++transitions > maxTransitionsPerRun) {
				ProcessRecord record = store.find(instanceId)
						.orElseThrow(() -> new ProcessInstanceNotFoundException(instanceId));
				record.status(ProcessStatus.FAILED);
				store.save(record);
				throw new ProcessExecutionException("process instance " + instanceId + " exceeded "
						+ maxTransitionsPerRun + " transitions in one run (likely a cycle in the definition)");
			}
		}
	}

	private ProcessInstanceView toView(ProcessRecord r) {
		return new ProcessInstanceView(r.id(), r.definitionKey(), r.currentStep(), r.status(),
				codec.toNode(r.contextJson()), r.createdAt(), r.updatedAt());
	}
}
