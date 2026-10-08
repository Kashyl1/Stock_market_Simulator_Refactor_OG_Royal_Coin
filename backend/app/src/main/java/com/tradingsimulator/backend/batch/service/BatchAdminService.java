package com.tradingsimulator.backend.batch.service;

import java.time.Instant;
import java.util.List;

import com.tradingsimulator.backend.batch.BatchItemStatus;
import com.tradingsimulator.backend.batch.BatchTypeCode;
import com.tradingsimulator.backend.web.PageResponse;

public interface BatchAdminService {

	List<BatchTypeView> types();

	PageResponse<BatchJobView> jobs(BatchJobFilter filter, int page, int size);

	BatchJobView job(Long jobId);

	PageResponse<BatchItemView> items(Long jobId, BatchItemStatus status, int page, int size);

	BatchJobView plan(BatchTypeCode type, Instant scheduledFor);

	void remove(Long jobId);

	void stop(Long jobId);

	BatchJobView acknowledge(Long jobId);
}
