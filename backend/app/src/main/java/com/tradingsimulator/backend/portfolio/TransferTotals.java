package com.tradingsimulator.backend.portfolio;

import java.math.BigDecimal;

public record TransferTotals(Long portfolioId, TransferDirection direction, BigDecimal mainAmount) {
}
