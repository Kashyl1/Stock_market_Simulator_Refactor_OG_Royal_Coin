package com.tradingsimulator.processengine.store;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.tradingsimulator.processengine.ProcessStatus;
import com.tradingsimulator.processengine.StepLogEntry;

public interface ProcessInstanceStore {

	ProcessRecord create(String definitionKey, String startStep, String contextJson);

	Optional<ProcessRecord> find(UUID id);

	void save(ProcessRecord record);

	int stepCount(UUID instanceId);

	void appendLog(StepLogAppend entry);

	List<StepLogEntry> log(UUID instanceId);

	Optional<String> latestContextBefore(UUID instanceId, String step);
}
