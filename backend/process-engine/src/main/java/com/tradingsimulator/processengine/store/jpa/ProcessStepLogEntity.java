package com.tradingsimulator.processengine.store.jpa;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "process_step_log")
public class ProcessStepLogEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "instance_id", nullable = false)
	private UUID instanceId;

	@Column(name = "sequence_no", nullable = false)
	private int sequenceNo;

	@Column(nullable = false)
	private String step;

	@Column
	private String outcome;

	@Column(nullable = false)
	private String disposition;

	@Column(name = "started_at", nullable = false)
	private Instant startedAt;

	@Column(name = "finished_at", nullable = false)
	private Instant finishedAt;

	@Column(columnDefinition = "text")
	private String error;

	@Column(name = "context_before", columnDefinition = "text")
	private String contextBefore;

	protected ProcessStepLogEntity() {
	}

	ProcessStepLogEntity(UUID instanceId, int sequenceNo, String step, String outcome, String disposition,
			Instant startedAt, Instant finishedAt, String error, String contextBefore) {
		this.instanceId = instanceId;
		this.sequenceNo = sequenceNo;
		this.step = step;
		this.outcome = outcome;
		this.disposition = disposition;
		this.startedAt = startedAt;
		this.finishedAt = finishedAt;
		this.error = error;
		this.contextBefore = contextBefore;
	}

	int getSequenceNo() {
		return sequenceNo;
	}

	String getStep() {
		return step;
	}

	String getOutcome() {
		return outcome;
	}

	String getDisposition() {
		return disposition;
	}

	Instant getStartedAt() {
		return startedAt;
	}

	Instant getFinishedAt() {
		return finishedAt;
	}

	String getError() {
		return error;
	}

	String getContextBefore() {
		return contextBefore;
	}
}
