package com.tradingsimulator.backend.dev;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.tradingsimulator.backend.auth.Role;
import com.tradingsimulator.backend.auth.User;
import com.tradingsimulator.backend.auth.UserRepository;
import com.tradingsimulator.backend.auth.UserStatus;
import com.tradingsimulator.backend.support.TestEntities;
import com.tradingsimulator.backend.support.TestUsers;
import com.tradingsimulator.backend.wallet.Wallet;
import com.tradingsimulator.backend.wallet.WalletRepository;

class DevAccountSeederTest {

	private final UserRepository users = mock(UserRepository.class);
	private final WalletRepository wallets = mock(WalletRepository.class);
	private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);

	private final DevAccountSeeder seeder = new DevAccountSeeder(users, wallets, passwordEncoder);

	@Test
	void createsAnActiveAdminWithAWalletOnTheFirstStart() {
		when(users.existsByEmailIgnoreCase(DevAccountSeeder.ADMIN_EMAIL)).thenReturn(false);
		when(passwordEncoder.encode(DevAccountSeeder.ADMIN_PASSWORD)).thenReturn(TestUsers.PASSWORD_HASH);
		when(users.save(any(User.class))).thenAnswer(invocation -> TestEntities.withId(invocation.getArgument(0), TestUsers.USER_ID));

		seeder.run(new DefaultApplicationArguments());

		ArgumentCaptor<User> admin = ArgumentCaptor.forClass(User.class);
		verify(users).save(admin.capture());
		assertThat(admin.getValue().getEmail()).isEqualTo(DevAccountSeeder.ADMIN_EMAIL);
		assertThat(admin.getValue().getPasswordHash()).isEqualTo(TestUsers.PASSWORD_HASH);
		assertThat(admin.getValue().getRole()).isEqualTo(Role.ADMIN);
		assertThat(admin.getValue().getStatus()).isEqualTo(UserStatus.ACTIVE);

		ArgumentCaptor<Wallet> wallet = ArgumentCaptor.forClass(Wallet.class);
		verify(wallets).save(wallet.capture());
		assertThat(wallet.getValue().getUserId()).isEqualTo(TestUsers.USER_ID);
	}

	@Test
	void leavesAnExistingAdminAlone() {
		when(users.existsByEmailIgnoreCase(DevAccountSeeder.ADMIN_EMAIL)).thenReturn(true);

		seeder.run(new DefaultApplicationArguments());

		verify(users).existsByEmailIgnoreCase(DevAccountSeeder.ADMIN_EMAIL);
		verifyNoInteractions(wallets, passwordEncoder);
	}
}
