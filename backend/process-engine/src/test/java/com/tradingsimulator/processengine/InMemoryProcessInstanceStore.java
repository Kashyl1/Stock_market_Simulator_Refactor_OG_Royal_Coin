package com.tradingsimulator.processengine;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.tradingsimulator.processengine.store.ProcessInstanceStore;
import com.tradingsimulator.processengine.store.ProcessRecord;
import com.tradingsimulator.processengine.store.StepLogAppend;

final class InMemoryProcessInstanceStore implements ProcessInstanceStore {

	private final Map<UUID, ProcessRecord> records = new LinkedHashMap<>();
	private final Map<UUID, List<StepLogAppend>> logs = new LinkedHashMap<>();

	@Override
	public ProcessRecord create(String definitionKey, String startStep, String contextJson) {
		Instant now = Instant.now();
		ProcessRecord record = new ProcessRecord(UUID.randomUUID(), definitionKey, startStep,
				ProcessStatus.RUNNING, contextJson, now, now);
		records.put(record.id(), record);
		logs.put(record.id(), new ArrayList<>());
		return record;
	}

	@Override
	public Optional<ProcessRecord> find(UUID id) {
		return Optional.ofNullable(records.get(id));
	}

	@Override
	public void save(ProcessRecord record) {
		records.put(record.id(), record);
	}

	@Override
	public int stepCount(UUID instanceId) {
		return logs.getOrDefault(instanceId, List.of()).size();
	}

	@Override
	public void appendLog(StepLogAppend entry) {
		logs.computeIfAbsent(entry.instanceId(), k -> new ArrayList<>()).add(entry);
	}

	@Override
	public List<StepLogEntry> log(UUID instanceId) {
		return logs.getOrDefault(instanceId, List.of()).stream()
				.map(e -> new StepLogEntry(e.sequenceNo(), e.step(), e.outcome(), e.disposition(),
						e.startedAt(), e.finishedAt(), e.error()))
				.toList();
	}

	@Override
	public Optional<String> latestContextBefore(UUID instanceId, String step) {
		List<StepLogAppend> entries = logs.getOrDefault(instanceId, List.of());
		for (int i = entries.size() - 1; i >= 0; i--) {
			if (entries.get(i).step().equals(step)) {
				return Optional.ofNullable(entries.get(i).contextBefore());
			}
		}
		return Optional.empty();
	}

	List<StepLogAppend> rawLog(UUID instanceId) {
		return List.copyOf(logs.getOrDefault(instanceId, List.of()));
	}
}
