package com.tradingsimulator.backend.batch.service;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonRawValue;
import com.tradingsimulator.backend.batch.BatchItem;
import com.tradingsimulator.backend.batch.BatchItemStatus;

public record BatchItemView(Long id, String key, BatchItemStatus status, @JsonRawValue String payload, String errorCode, String errorMessage,
		Instant processedAt) {

	static BatchItemView of(BatchItem item) {
		return new BatchItemView(item.getId(), item.getItemKey(), item.getStatus(), item.getPayload(), item.getErrorCode(), item.getErrorMessage(),
				item.getProcessedAt());
	}
}
