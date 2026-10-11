package com.tradingsimulator.backend.portfolio.service;

import java.math.BigDecimal;
import java.time.Instant;

import com.tradingsimulator.backend.portfolio.PortfolioTransfer;
import com.tradingsimulator.backend.portfolio.TransferDirection;

public record TransferView(Long id, TransferDirection direction, BigDecimal amount, String currency, BigDecimal mainAmount, String mainCurrency,
		BigDecimal fxRate, BigDecimal fxFeeAmount, Instant transferredAt) {

	static TransferView of(PortfolioTransfer transfer, String currency, String mainCurrency) {
		return new TransferView(transfer.getId(), transfer.getDirection(), transfer.getAmount(), currency, transfer.getMainAmount(), mainCurrency,
				transfer.getFxRate(), transfer.getFxFeeAmount(), transfer.getTransferredAt());
	}
}
