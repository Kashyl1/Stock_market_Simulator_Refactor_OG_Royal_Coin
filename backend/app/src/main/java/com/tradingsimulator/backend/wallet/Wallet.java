package com.tradingsimulator.backend.wallet;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "wallet")
public class Wallet extends WalletJpa {
}
