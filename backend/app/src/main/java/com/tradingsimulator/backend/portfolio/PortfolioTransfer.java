package com.tradingsimulator.backend.portfolio;

import java.time.Instant;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "portfolio_transfer")
public class PortfolioTransfer extends PortfolioTransferJpa {

	public static PortfolioTransfer of(Long portfolioId, TransferQuote quote, Instant transferredAt) {
		PortfolioTransfer transfer = new PortfolioTransfer();
		transfer.setPortfolioId(portfolioId);
		transfer.setDirection(quote.direction());
		transfer.setAmount(quote.amount());
		transfer.setMainAmount(quote.mainAmount());
		transfer.setFxRate(quote.fxRate());
		transfer.setFxFeeAmount(quote.fxFeeAmount());
		transfer.setTransferredAt(transferredAt);
		return transfer;
	}
}
