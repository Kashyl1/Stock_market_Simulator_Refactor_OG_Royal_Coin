package com.tradingsimulator.backend.currency;

import java.math.BigDecimal;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

@Validated
@ConfigurationProperties(prefix = FeeProperties.PREFIX)
public record FeeProperties(@NotNull @PositiveOrZero BigDecimal fxConversionRate) {

	public static final String PREFIX = "app.fees";
}
