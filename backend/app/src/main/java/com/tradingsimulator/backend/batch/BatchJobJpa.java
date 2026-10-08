package com.tradingsimulator.backend.batch;

import java.time.Instant;

import com.tradingsimulator.backend.common.persistence.AbstractEntity;

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

@MappedSuperclass
@Getter
@Setter
public class BatchJobJpa extends AbstractEntity {

	private static final int STATUS_MAX_LENGTH = 30;
	private static final int ORIGIN_MAX_LENGTH = 10;
	private static final int ACTOR_MAX_LENGTH = 100;

	@Column(name = "batch_type_id", nullable = false)
	private Long batchTypeId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = STATUS_MAX_LENGTH)
	private BatchJobStatus status;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = ORIGIN_MAX_LENGTH)
	private BatchJobOrigin origin;

	@Column(name = "scheduled_for", nullable = false)
	private Instant scheduledFor;

	@Column(name = "started_at")
	private Instant startedAt;

	@Column(name = "finished_at")
	private Instant finishedAt;

	@Column(name = "total_items")
	private Integer totalItems;

	@Column(name = "succeeded_items", nullable = false)
	private int succeededItems;

	@Column(name = "failed_items", nullable = false)
	private int failedItems;

	@Column(name = "stop_requested_by", length = ACTOR_MAX_LENGTH)
	private String stopRequestedBy;

	@Column(name = "stop_requested_at")
	private Instant stopRequestedAt;

	@Column(name = "error_code", length = BatchFailure.CODE_MAX_LENGTH)
	private String errorCode;

	@Column(name = "error_message", length = BatchFailure.MESSAGE_MAX_LENGTH)
	private String errorMessage;

	@Column(name = "acknowledged_by", length = ACTOR_MAX_LENGTH)
	private String acknowledgedBy;

	@Column(name = "acknowledged_at")
	private Instant acknowledgedAt;
}
