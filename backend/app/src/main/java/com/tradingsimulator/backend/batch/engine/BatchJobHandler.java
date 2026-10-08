package com.tradingsimulator.backend.batch.engine;

import com.tradingsimulator.backend.batch.BatchTypeCode;

public interface BatchJobHandler {

	BatchTypeCode type();

	void run(BatchRun run);
}
