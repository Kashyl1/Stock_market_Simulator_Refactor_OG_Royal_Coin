package com.tradingsimulator.backend.batch.engine;

public interface BatchRun {

	void totalItems(int total);

	void item(String key, BatchItemWork work);
}
