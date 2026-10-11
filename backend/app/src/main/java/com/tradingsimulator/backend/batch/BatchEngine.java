package com.tradingsimulator.backend.batch;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "batch_engine")
public class BatchEngine extends BatchEngineJpa {

	public static BatchEngine active() {
		return new BatchEngine();
	}

	public void pause() {
		setPaused(true);
	}

	public void resume() {
		setPaused(false);
	}
}
