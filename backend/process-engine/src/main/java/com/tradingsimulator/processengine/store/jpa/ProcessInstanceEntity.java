package com.tradingsimulator.processengine.store.jpa;

import java.time.Instant;
import java.util.UUID;

import com.tradingsimulator.processengine.ProcessStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "process_instance")
public class ProcessInstanceEntity {

	@Id
	private UUID id;

	@Column(name = "definition_key", nullable = false, updatable = false)
	private String definitionKey;

	@Column(name = "current_step", nullable = false)
	private String currentStep;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private ProcessStatus status;

	@Column(name = "context_json", nullable = false, columnDefinition = "text")
	private String contextJson;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Version
	private long version;

	protected ProcessInstanceEntity() {
	}

	ProcessInstanceEntity(UUID id, String definitionKey, String currentStep, ProcessStatus status,
			String contextJson, Instant createdAt, Instant updatedAt) {
		this.id = id;
		this.definitionKey = definitionKey;
		this.currentStep = currentStep;
		this.status = status;
		this.contextJson = contextJson;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
	}

	UUID getId() {
		return id;
	}

	String getDefinitionKey() {
		return definitionKey;
	}

	String getCurrentStep() {
		return currentStep;
	}

	void setCurrentStep(String currentStep) {
		this.currentStep = currentStep;
	}

	ProcessStatus getStatus() {
		return status;
	}

	void setStatus(ProcessStatus status) {
		this.status = status;
	}

	String getContextJson() {
		return contextJson;
	}

	void setContextJson(String contextJson) {
		this.contextJson = contextJson;
	}

	Instant getCreatedAt() {
		return createdAt;
	}

	Instant getUpdatedAt() {
		return updatedAt;
	}

	void setUpdatedAt(Instant updatedAt) {
		this.updatedAt = updatedAt;
	}
}
