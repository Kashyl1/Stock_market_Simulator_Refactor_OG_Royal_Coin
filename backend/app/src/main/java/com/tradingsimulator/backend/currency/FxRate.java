package com.tradingsimulator.backend.currency;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "fx_rate")
public class FxRate extends FxRateJpa {
}
