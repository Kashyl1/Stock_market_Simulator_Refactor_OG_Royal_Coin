package com.tradingsimulator.backend.portfolio.service;

import java.util.List;

public interface PortfolioService {

	List<PortfolioView> portfolios(Long userId);

	PortfolioView portfolio(Long userId, Long portfolioId);

	PortfolioView create(Long userId, String name, String currency);
}
