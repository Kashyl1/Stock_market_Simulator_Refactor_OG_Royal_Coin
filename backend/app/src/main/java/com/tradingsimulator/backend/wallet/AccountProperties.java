package com.tradingsimulator.backend.wallet;

import java.math.BigDecimal;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

@Validated
@ConfigurationProperties(prefix = AccountProperties.PREFIX)
public record AccountProperties(@NotBlank String currency, @NotNull @PositiveOrZero BigDecimal startingBalance) {

	public static final String PREFIX = "app.account";
}
