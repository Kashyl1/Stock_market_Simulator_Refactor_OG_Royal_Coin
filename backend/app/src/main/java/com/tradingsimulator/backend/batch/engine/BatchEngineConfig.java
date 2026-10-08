package com.tradingsimulator.backend.batch.engine;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.tradingsimulator.backend.batch.BatchProperties;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(BatchProperties.ENABLED)
@EnableScheduling
public class BatchEngineConfig {

	public static final String BATCH_WORKERS = "batchWorkers";

	private static final String WORKER_NAME_PREFIX = "batch-job-";
	private static final long FIRST_WORKER_NUMBER = 1;
	private static final String NO_INFERRED_DESTROY_METHOD = "";

	@Bean(name = BATCH_WORKERS, destroyMethod = NO_INFERRED_DESTROY_METHOD)
	ExecutorService batchWorkers() {
		return Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name(WORKER_NAME_PREFIX, FIRST_WORKER_NUMBER).factory());
	}
}
