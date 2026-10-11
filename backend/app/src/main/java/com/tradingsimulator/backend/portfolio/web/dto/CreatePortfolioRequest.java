package com.tradingsimulator.backend.portfolio.web.dto;

import com.tradingsimulator.backend.currency.CurrencyJpa;
import com.tradingsimulator.backend.portfolio.Portfolio;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreatePortfolioRequest(@NotBlank @Size(max = Portfolio.NAME_MAX_LENGTH) String name,
		@NotBlank @Size(min = CurrencyJpa.CODE_LENGTH, max = CurrencyJpa.CODE_LENGTH) String currency) {
}
