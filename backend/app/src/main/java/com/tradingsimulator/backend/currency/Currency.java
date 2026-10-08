package com.tradingsimulator.backend.currency;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "currency")
public class Currency extends CurrencyJpa {
}
