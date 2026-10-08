package com.tradingsimulator.backend.batch.engine;

import java.time.Clock;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.tradingsimulator.backend.batch.BatchProperties;
import com.tradingsimulator.backend.batch.BatchStopRequest;
import com.tradingsimulator.backend.common.persistence.JpaAuditingConfig;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;

@Component
@ConditionalOnProperty(BatchProperties.ENABLED)
@Slf4j
public class BatchPoller {

	private final BatchPlanner planner;
	private final BatchRunner runner;
	private final RunningBatchJobs running;
	private final ExecutorService workers;
	private final BatchProperties properties;
	private final Clock clock;
	private final AtomicBoolean recovered = new AtomicBoolean();

	public BatchPoller(BatchPlanner planner, BatchRunner runner, RunningBatchJobs running, @Qualifier(BatchEngineConfig.BATCH_WORKERS) ExecutorService workers,
			BatchProperties properties, Clock clock) {
		this.planner = planner;
		this.runner = runner;
		this.running = running;
		this.workers = workers;
		this.properties = properties;
		this.clock = clock;
	}

	@Scheduled(fixedDelayString = BatchProperties.POLL_INTERVAL)
	public void poll() {
		if (!recovered.get()) {
			planner.recover();
			recovered.set(true);
		}
		planner.startDueJobs().forEach(jobId -> workers.execute(() -> runner.run(jobId)));
	}

	@PreDestroy
	void stopRunningJobs() throws InterruptedException {
		running.requestStopOfAll(new BatchStopRequest(JpaAuditingConfig.SYSTEM_AUDITOR, clock.instant()));
		workers.shutdown();
		if (!workers.awaitTermination(properties.shutdownTimeout().toMillis(), TimeUnit.MILLISECONDS)) {
			log.warn("Batch jobs still running after {}; they will be marked as failed on the next start", properties.shutdownTimeout());
		}
	}
}
