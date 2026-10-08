package com.tradingsimulator.backend.batch.engine;

import java.util.List;

public interface BatchPlanner {

	void recover();

	List<Long> startDueJobs();

	void remove(Long jobId);
}
