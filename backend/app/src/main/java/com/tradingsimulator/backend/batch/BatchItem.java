package com.tradingsimulator.backend.batch;

import java.time.Instant;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "batch_item")
public class BatchItem extends BatchItemJpa {

	public static BatchItem succeeded(Long batchJobId, String itemKey, String payload, Instant processedAt) {
		BatchItem item = processed(batchJobId, itemKey, BatchItemStatus.SUCCEEDED, processedAt);
		item.setPayload(payload);
		return item;
	}

	public static BatchItem failed(Long batchJobId, String itemKey, BatchFailure failure, Instant processedAt) {
		BatchItem item = processed(batchJobId, itemKey, BatchItemStatus.FAILED, processedAt);
		item.setErrorCode(failure.code());
		item.setErrorMessage(failure.message());
		return item;
	}

	private static BatchItem processed(Long batchJobId, String itemKey, BatchItemStatus status, Instant processedAt) {
		BatchItem item = new BatchItem();
		item.setBatchJobId(batchJobId);
		item.setItemKey(itemKey);
		item.setStatus(status);
		item.setProcessedAt(processedAt);
		return item;
	}
}
