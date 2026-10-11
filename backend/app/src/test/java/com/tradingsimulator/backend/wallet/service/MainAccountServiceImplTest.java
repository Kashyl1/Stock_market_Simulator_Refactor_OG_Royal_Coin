package com.tradingsimulator.backend.wallet.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.tradingsimulator.backend.support.TestFx;
import com.tradingsimulator.backend.support.TestMoney;
import com.tradingsimulator.backend.support.TestUsers;
import com.tradingsimulator.backend.wallet.Wallet;
import com.tradingsimulator.backend.wallet.WalletEntry;
import com.tradingsimulator.backend.wallet.WalletEntryReferenceType;
import com.tradingsimulator.backend.wallet.WalletEntryRepository;
import com.tradingsimulator.backend.wallet.WalletEntryType;
import com.tradingsimulator.backend.wallet.WalletRepository;

class MainAccountServiceImplTest {

	private final WalletRepository wallets = mock(WalletRepository.class);
	private final WalletEntryRepository entries = mock(WalletEntryRepository.class);

	private final MainAccountService service = new MainAccountServiceImpl(wallets, entries, TestMoney.accountProperties(), TestMoney.CLOCK);

	@Test
	void opensAnEmptyMainAccountInTheConfiguredCurrency() {
		service.open(TestUsers.USER_ID);

		ArgumentCaptor<Wallet> wallet = ArgumentCaptor.forClass(Wallet.class);
		verify(wallets).save(wallet.capture());
		assertThat(wallet.getValue().getUserId()).isEqualTo(TestUsers.USER_ID);
		assertThat(wallet.getValue().isMainAccount()).isTrue();
		assertThat(wallet.getValue().getCurrency()).isEqualTo(TestFx.PLN);
		assertThat(wallet.getValue().getCashBalance()).isEqualByComparingTo(BigDecimal.ZERO);
	}

	@Test
	void grantsTheStartingBalanceAsADeposit() {
		Wallet mainAccount = TestMoney.mainAccount(BigDecimal.ZERO);
		when(wallets.mainAccountOf(TestUsers.USER_ID)).thenReturn(mainAccount);

		service.grantStartingBalance(TestUsers.USER_ID);

		ArgumentCaptor<WalletEntry> entry = ArgumentCaptor.forClass(WalletEntry.class);
		verify(entries).save(entry.capture());
		assertThat(mainAccount.getCashBalance()).isEqualByComparingTo(TestMoney.STARTING_BALANCE);
		assertThat(entry.getValue().getEntryType()).isEqualTo(WalletEntryType.DEPOSIT);
		assertThat(entry.getValue().getAmount()).isEqualByComparingTo(TestMoney.STARTING_BALANCE);
		assertThat(entry.getValue().getReferenceType()).isEqualTo(WalletEntryReferenceType.STARTING_BALANCE);
		assertThat(entry.getValue().getEffectiveAt()).isEqualTo(TestMoney.NOW);
	}

	@Test
	void grantsTheStartingBalanceOnlyOnce() {
		Wallet mainAccount = TestMoney.mainAccount(TestMoney.STARTING_BALANCE);
		when(wallets.mainAccountOf(TestUsers.USER_ID)).thenReturn(mainAccount);
		when(entries.existsByWalletIdAndReferenceType(TestMoney.MAIN_ACCOUNT_ID, WalletEntryReferenceType.STARTING_BALANCE)).thenReturn(true);

		service.grantStartingBalance(TestUsers.USER_ID);

		verify(entries, never()).save(any(WalletEntry.class));
		assertThat(mainAccount.getCashBalance()).isEqualByComparingTo(TestMoney.STARTING_BALANCE);
	}
}
