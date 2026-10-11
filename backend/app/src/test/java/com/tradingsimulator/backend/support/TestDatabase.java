package com.tradingsimulator.backend.support;

import com.tradingsimulator.backend.batch.BatchProperties;

public final class TestDatabase {

	public static final String POSTGRES_IMAGE = "postgres:18-alpine";
	public static final String MIGRATIONS = "classpath:db/migration";
	public static final String FLYWAY_ENABLED = "spring.flyway.enabled=true";
	public static final String HIBERNATE_VALIDATE = "spring.jpa.hibernate.ddl-auto=validate";
	public static final String BATCH_ENGINE_OFF = BatchProperties.ENABLED + "=false";

	private TestDatabase() {
	}
}
