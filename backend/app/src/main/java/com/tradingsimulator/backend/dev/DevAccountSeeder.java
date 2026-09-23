package com.tradingsimulator.backend.dev;

import org.jspecify.annotations.NonNull;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.tradingsimulator.backend.auth.Role;
import com.tradingsimulator.backend.auth.User;
import com.tradingsimulator.backend.auth.UserRepository;
import com.tradingsimulator.backend.config.Profiles;
import com.tradingsimulator.backend.wallet.Wallet;
import com.tradingsimulator.backend.wallet.WalletRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Profile(Profiles.DEV)
@RequiredArgsConstructor
@Slf4j
public class DevAccountSeeder implements ApplicationRunner {

	public static final String ADMIN_EMAIL = "admin@admin.com";
	public static final String ADMIN_PASSWORD = "admin";

	private static final String ADMIN_DISPLAY_NAME = "Admin";

	private final UserRepository users;
	private final WalletRepository wallets;
	private final PasswordEncoder passwordEncoder;

	@Override
	@Transactional
	public void run(@NonNull ApplicationArguments arguments) {
		if (users.existsByEmailIgnoreCase(ADMIN_EMAIL)) {
			log.info("Dev admin account already present: {}", ADMIN_EMAIL);
			return;
		}

		User admin = users.save(User.active(ADMIN_EMAIL, passwordEncoder.encode(ADMIN_PASSWORD), ADMIN_DISPLAY_NAME, Role.ADMIN));
		wallets.save(Wallet.empty(admin.getId()));
		log.info("Dev admin account created: {} / {}", ADMIN_EMAIL, ADMIN_PASSWORD);
	}
}
