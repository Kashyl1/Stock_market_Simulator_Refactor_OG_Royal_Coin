package com.tradingsimulator.backend.portfolio;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public record TransferSums(BigDecimal contributed, BigDecimal withdrawn, BigDecimal putIn, BigDecimal takenOut) {

	public static TransferSums of(Collection<TransferTotals> totals) {
		Map<TransferDirection, TransferTotals> byDirection = totals.stream().collect(Collectors.toMap(TransferTotals::direction, Function.identity()));
		TransferTotals in = byDirection.getOrDefault(TransferDirection.IN, none(TransferDirection.IN));
		TransferTotals out = byDirection.getOrDefault(TransferDirection.OUT, none(TransferDirection.OUT));
		return new TransferSums(in.amount(), out.amount(), in.mainAmount(), out.mainAmount());
	}

	private static TransferTotals none(TransferDirection direction) {
		return new TransferTotals(null, direction, BigDecimal.ZERO, BigDecimal.ZERO);
	}
}
