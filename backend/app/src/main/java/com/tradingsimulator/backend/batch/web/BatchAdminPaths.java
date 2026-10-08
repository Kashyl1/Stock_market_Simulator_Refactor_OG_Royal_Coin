package com.tradingsimulator.backend.batch.web;

import com.tradingsimulator.backend.web.ApiPaths;

public final class BatchAdminPaths {

	public static final String BASE = ApiPaths.API + "/admin/batch";
	public static final String TYPES = "/types";
	public static final String JOBS = "/jobs";
	public static final String JOB = JOBS + "/{id}";
	public static final String JOB_ITEMS = JOB + "/items";
	public static final String JOB_STOP = JOB + "/stop";
	public static final String JOB_ACKNOWLEDGE = JOB + "/acknowledge";

	private BatchAdminPaths() {
	}
}
