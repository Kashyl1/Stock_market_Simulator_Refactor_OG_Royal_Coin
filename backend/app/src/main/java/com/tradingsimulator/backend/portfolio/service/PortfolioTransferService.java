package com.tradingsimulator.backend.portfolio.service;

import java.math.BigDecimal;

import com.tradingsimulator.backend.portfolio.TransferDirection;
import com.tradingsimulator.backend.web.PageResponse;

public interface PortfolioTransferService {

	TransferQuoteView quote(Long userId, Long portfolioId, TransferDirection direction, BigDecimal amount);

	TransferView transfer(Long userId, Long portfolioId, TransferDirection direction, BigDecimal amount);

	PageResponse<TransferView> transfers(Long userId, Long portfolioId, int page, int size);
}
