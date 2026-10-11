package com.tradingsimulator.backend.portfolio.web.dto;

import java.math.BigDecimal;

import com.tradingsimulator.backend.portfolio.TransferDirection;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record TransferRequest(@NotNull TransferDirection direction, @NotNull @Positive BigDecimal amount) {
}
