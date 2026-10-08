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
public class BatchItemJpa extends AbstractEntity {

	private static final int KEY_MAX_LENGTH = 100;
	private static final int STATUS_MAX_LENGTH = 20;

	@Column(name = "batch_job_id", nullable = false)
	private Long batchJobId;

	@Column(name = "item_key", nullable = false, length = KEY_MAX_LENGTH)
	private String itemKey;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = STATUS_MAX_LENGTH)
	private BatchItemStatus status;

	@Column(columnDefinition = "text")
	private String payload;

	@Column(name = "error_code", length = BatchFailure.CODE_MAX_LENGTH)
	private String errorCode;

	@Column(name = "error_message", length = BatchFailure.MESSAGE_MAX_LENGTH)
	private String errorMessage;

	@Column(name = "processed_at", nullable = false)
	private Instant processedAt;
}
