package com.tradingsimulator.backend.portfolio;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

public record TransferSums(BigDecimal putIn, BigDecimal takenOut) {

	public static TransferSums of(Collection<TransferTotals> totals) {
		Map<TransferDirection, BigDecimal> byDirection = totals.stream().collect(Collectors.toMap(TransferTotals::direction, TransferTotals::mainAmount));
		return new TransferSums(byDirection.getOrDefault(TransferDirection.IN, BigDecimal.ZERO),
				byDirection.getOrDefault(TransferDirection.OUT, BigDecimal.ZERO));
	}
}
