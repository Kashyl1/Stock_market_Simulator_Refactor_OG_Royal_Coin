package com.tradingsimulator.backend.portfolio.web;

import com.tradingsimulator.backend.web.ApiPaths;

public final class PortfolioPaths {

	public static final String BASE = ApiPaths.API + "/portfolios";
	public static final String PORTFOLIO = "/{id}";
	public static final String TRANSFERS = PORTFOLIO + "/transfers";
	public static final String TRANSFER_QUOTE = TRANSFERS + "/quote";

	private PortfolioPaths() {
	}
}
