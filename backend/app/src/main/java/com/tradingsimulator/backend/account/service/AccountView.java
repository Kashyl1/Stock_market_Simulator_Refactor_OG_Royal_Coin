package com.tradingsimulator.backend.account.service;

import java.math.BigDecimal;

import com.tradingsimulator.backend.portfolio.Performance;

public record AccountView(String currency, BigDecimal cashBalance, BigDecimal available, Performance performance) {
}
