package com.tradingsimulator.processengine.store.jpa;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.tradingsimulator.processengine.ProcessStatus;
import com.tradingsimulator.processengine.StepLogEntry;
import com.tradingsimulator.processengine.store.ProcessInstanceStore;
import com.tradingsimulator.processengine.store.ProcessRecord;
import com.tradingsimulator.processengine.store.StepLogAppend;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class JpaProcessInstanceStore implements ProcessInstanceStore {

	private final ProcessInstanceRepository instances;
	private final ProcessStepLogRepository stepLogs;

	@Override
	public ProcessRecord create(String definitionKey, String startStep, String contextJson) {
		Instant now = Instant.now();
		ProcessInstanceEntity entity = new ProcessInstanceEntity(
				UUID.randomUUID(), definitionKey, startStep, ProcessStatus.RUNNING, contextJson, now, now);
		return toRecord(instances.save(entity));
	}

	@Override
	public Optional<ProcessRecord> find(UUID id) {
		return instances.findById(id).map(JpaProcessInstanceStore::toRecord);
	}

	@Override
	public void save(ProcessRecord record) {
		ProcessInstanceEntity entity = instances.findById(record.id())
				.orElseThrow(() -> new IllegalStateException("process instance vanished: " + record.id()));
		entity.setCurrentStep(record.currentStep());
		entity.setStatus(record.status());
		entity.setContextJson(record.contextJson());
		entity.setUpdatedAt(record.updatedAt() != null ? record.updatedAt() : Instant.now());
		instances.save(entity);
	}

	@Override
	public int stepCount(UUID instanceId) {
		return stepLogs.countByInstanceId(instanceId);
	}

	@Override
	public void appendLog(StepLogAppend e) {
		stepLogs.save(new ProcessStepLogEntity(
				e.instanceId(), e.sequenceNo(), e.step(), e.outcome(), e.disposition(),
				e.startedAt(), e.finishedAt(), e.error(), e.contextBefore()));
	}

	@Override
	public List<StepLogEntry> log(UUID instanceId) {
		return stepLogs.findByInstanceIdOrderBySequenceNoAsc(instanceId).stream()
				.map(e -> new StepLogEntry(e.getSequenceNo(), e.getStep(), e.getOutcome(), e.getDisposition(),
						e.getStartedAt(), e.getFinishedAt(), e.getError()))
				.toList();
	}

	@Override
	public Optional<String> latestContextBefore(UUID instanceId, String step) {
		return stepLogs.findFirstByInstanceIdAndStepOrderBySequenceNoDesc(instanceId, step)
				.map(ProcessStepLogEntity::getContextBefore);
	}

	private static ProcessRecord toRecord(ProcessInstanceEntity e) {
		return new ProcessRecord(e.getId(), e.getDefinitionKey(), e.getCurrentStep(), e.getStatus(),
				e.getContextJson(), e.getCreatedAt(), e.getUpdatedAt());
	}
}
