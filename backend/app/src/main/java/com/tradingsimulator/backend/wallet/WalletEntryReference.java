package com.tradingsimulator.backend.wallet;

public record WalletEntryReference(WalletEntryReferenceType type, Long id) {

	public static WalletEntryReference startingBalance() {
		return new WalletEntryReference(WalletEntryReferenceType.STARTING_BALANCE, null);
	}

	public static WalletEntryReference portfolioTransfer(Long transferId) {
		return new WalletEntryReference(WalletEntryReferenceType.PORTFOLIO_TRANSFER, transferId);
	}
}
