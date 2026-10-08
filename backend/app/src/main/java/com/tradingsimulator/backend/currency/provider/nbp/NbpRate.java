package com.tradingsimulator.backend.currency.provider.nbp;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonProperty;

record NbpRate(String code, @JsonProperty(NbpRate.MID) BigDecimal midRate) {

	static final String MID = "mid";
}
