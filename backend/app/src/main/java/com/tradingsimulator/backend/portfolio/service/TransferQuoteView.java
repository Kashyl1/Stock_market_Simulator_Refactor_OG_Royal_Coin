package com.tradingsimulator.backend.portfolio.service;

import java.math.BigDecimal;

import com.tradingsimulator.backend.portfolio.TransferDirection;
import com.tradingsimulator.backend.portfolio.TransferQuote;

public record TransferQuoteView(TransferDirection direction, BigDecimal amount, String currency, BigDecimal mainAmount, String mainCurrency, BigDecimal fxRate,
		BigDecimal fxFeeAmount) {

	static TransferQuoteView of(TransferQuote quote, String currency, String mainCurrency) {
		return new TransferQuoteView(quote.direction(), quote.amount(), currency, quote.mainAmount(), mainCurrency, quote.fxRate(), quote.fxFeeAmount());
	}
}
