package com.tradingsimulator.backend.wallet.service;

public interface MainAccountService {

	void open(Long userId);

	void grantStartingBalance(Long userId);
}
