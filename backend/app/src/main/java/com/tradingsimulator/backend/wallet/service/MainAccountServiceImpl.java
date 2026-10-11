package com.tradingsimulator.backend.wallet.service;

import java.time.Clock;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tradingsimulator.backend.wallet.AccountProperties;
import com.tradingsimulator.backend.wallet.Wallet;
import com.tradingsimulator.backend.wallet.WalletEntryReference;
import com.tradingsimulator.backend.wallet.WalletEntryReferenceType;
import com.tradingsimulator.backend.wallet.WalletEntryRepository;
import com.tradingsimulator.backend.wallet.WalletEntryType;
import com.tradingsimulator.backend.wallet.WalletRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class MainAccountServiceImpl implements MainAccountService {

	private final WalletRepository wallets;
	private final WalletEntryRepository entries;
	private final AccountProperties properties;
	private final Clock clock;

	@Override
	@Transactional
	public void open(Long userId) {
		wallets.save(Wallet.mainAccount(userId, properties.currency()));
	}

	@Override
	@Transactional
	public void grantStartingBalance(Long userId) {
		Wallet mainAccount = wallets.mainAccountOf(userId);
		if (entries.existsByWalletIdAndReferenceType(mainAccount.getId(), WalletEntryReferenceType.STARTING_BALANCE)) {
			log.warn("User {} already has their starting balance", userId);
			return;
		}
		entries.save(mainAccount.credit(WalletEntryType.DEPOSIT, properties.startingBalance(), WalletEntryReference.startingBalance(), clock.instant()));
	}
}
