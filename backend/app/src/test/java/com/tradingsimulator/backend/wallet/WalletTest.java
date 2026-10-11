package com.tradingsimulator.backend.wallet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.tradingsimulator.backend.common.error.AppException;
import com.tradingsimulator.backend.support.TestMoney;

class WalletTest {

	private static final BigDecimal RESERVED = new BigDecimal("700.00");
	private static final WalletEntryReference TRANSFER = WalletEntryReference.portfolioTransfer(TestMoney.TRANSFER_ID);

	@Test
	void aCreditRaisesTheBalanceAndRecordsAPositiveEntry() {
		Wallet wallet = TestMoney.mainAccount(TestMoney.STARTING_BALANCE);

		WalletEntry entry = wallet.credit(WalletEntryType.TRANSFER_IN, TestMoney.DEPOSITED_ZLOTY, TRANSFER, TestMoney.NOW);

		assertThat(wallet.getCashBalance()).isEqualByComparingTo("1400.00");
		assertThat(entry.getAmount()).isEqualByComparingTo(TestMoney.DEPOSITED_ZLOTY);
		assertThat(entry.getBalanceAfter()).isEqualByComparingTo(wallet.getCashBalance());
		assertThat(entry.getWalletId()).isEqualTo(TestMoney.MAIN_ACCOUNT_ID);
		assertThat(entry.getReferenceType()).isEqualTo(WalletEntryReferenceType.PORTFOLIO_TRANSFER);
		assertThat(entry.getReferenceId()).isEqualTo(TestMoney.TRANSFER_ID);
		assertThat(entry.getEffectiveAt()).isEqualTo(TestMoney.NOW);
	}

	@Test
	void aDebitLowersTheBalanceAndRecordsANegativeEntry() {
		Wallet wallet = TestMoney.mainAccount(TestMoney.STARTING_BALANCE);

		WalletEntry entry = wallet.debit(WalletEntryType.TRANSFER_OUT, TestMoney.DEPOSITED_ZLOTY, TRANSFER, TestMoney.NOW);

		assertThat(wallet.getCashBalance()).isEqualByComparingTo("600.00");
		assertThat(entry.getAmount()).isEqualByComparingTo(TestMoney.DEPOSITED_ZLOTY.negate());
		assertThat(entry.getBalanceAfter()).isEqualByComparingTo(wallet.getCashBalance());
	}

	@Test
	void aDebitCannotTakeReservedMoney() {
		Wallet wallet = TestMoney.mainAccount(TestMoney.STARTING_BALANCE);
		wallet.setReservedBalance(RESERVED);

		assertThatThrownBy(() -> wallet.debit(WalletEntryType.TRANSFER_OUT, TestMoney.DEPOSITED_ZLOTY, TRANSFER, TestMoney.NOW))
				.isInstanceOf(AppException.class).extracting(thrown -> ((AppException) thrown).errorCode()).isEqualTo(WalletError.INSUFFICIENT_FUNDS);
		assertThat(wallet.getCashBalance()).isEqualByComparingTo(TestMoney.STARTING_BALANCE);
	}

	@Test
	void onlyAWalletWithoutAPortfolioIsTheMainAccount() {
		assertThat(TestMoney.mainAccount(BigDecimal.ZERO).isMainAccount()).isTrue();
		assertThat(TestMoney.portfolioWallet(TestMoney.accountProperties().currency(), BigDecimal.ZERO).isMainAccount()).isFalse();
	}
}
