package com.tradingsimulator.backend.batch.engine;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.stereotype.Component;

import com.tradingsimulator.backend.batch.BatchStopRequest;

@Component
public class RunningBatchJobs {

	private final Map<Long, AtomicReference<BatchStopRequest>> stopRequests = new ConcurrentHashMap<>();

	public void register(Long jobId) {
		stopRequests.put(jobId, new AtomicReference<>());
	}

	public void unregister(Long jobId) {
		stopRequests.remove(jobId);
	}

	public boolean requestStop(Long jobId, BatchStopRequest request) {
		AtomicReference<BatchStopRequest> slot = stopRequests.get(jobId);
		if (slot == null) {
			return false;
		}
		slot.compareAndSet(null, request);
		return true;
	}

	public void requestStopOfAll(BatchStopRequest request) {
		stopRequests.values().forEach(slot -> slot.compareAndSet(null, request));
	}

	public Optional<BatchStopRequest> stopRequest(Long jobId) {
		return Optional.ofNullable(stopRequests.get(jobId)).map(AtomicReference::get);
	}
}
