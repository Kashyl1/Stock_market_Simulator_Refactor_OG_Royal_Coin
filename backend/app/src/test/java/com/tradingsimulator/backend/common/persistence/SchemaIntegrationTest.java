package com.tradingsimulator.backend.common.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.tradingsimulator.backend.auth.Role;
import com.tradingsimulator.backend.auth.User;
import com.tradingsimulator.backend.auth.UserRepository;
import com.tradingsimulator.backend.auth.UserStatus;
import com.tradingsimulator.backend.support.TestJwtKeys;
import com.tradingsimulator.backend.support.TestUsers;
import com.tradingsimulator.backend.wallet.Wallet;
import com.tradingsimulator.backend.wallet.WalletRepository;

@SpringBootTest(properties = { SchemaIntegrationTest.FLYWAY_ENABLED, SchemaIntegrationTest.HIBERNATE_VALIDATE })
@Testcontainers(disabledWithoutDocker = true)
class SchemaIntegrationTest {

	static final String FLYWAY_ENABLED = "spring.flyway.enabled=true";
	static final String HIBERNATE_VALIDATE = "spring.jpa.hibernate.ddl-auto=validate";

	private static final String POSTGRES_IMAGE = "postgres:18-alpine";
	private static final String STORED_EMAIL = TestUsers.EMAIL_IN_CAPITALS;
	private static final String LOOKUP_EMAIL = TestUsers.EMAIL;
	private static final String PASSWORD_HASH = "irrelevant-hash";
	private static final String DISPLAY_NAME = TestUsers.DISPLAY_NAME;

	@Container
	@ServiceConnection
	static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(POSTGRES_IMAGE);

	@Autowired
	private UserRepository users;

	@Autowired
	private WalletRepository wallets;

	@DynamicPropertySource
	static void jwtKeys(DynamicPropertyRegistry registry) {
		TestJwtKeys.register(registry);
	}

	@Test
	void migratedSchemaMatchesTheEntitiesAndFindsAUserIgnoringCase() {
		User saved = users.save(newUser());
		wallets.save(emptyWallet(saved.getId()));

		assertThat(users.findByEmailIgnoreCase(LOOKUP_EMAIL)).contains(saved);
		assertThat(users.existsByEmailIgnoreCase(LOOKUP_EMAIL)).isTrue();
		assertThat(wallets.findAll()).singleElement()
				.satisfies(wallet -> assertThat(wallet.getUserId()).isEqualTo(saved.getId()));
	}

	private static User newUser() {
		User user = new User();
		user.setEmail(STORED_EMAIL);
		user.setPasswordHash(PASSWORD_HASH);
		user.setDisplayName(DISPLAY_NAME);
		user.setRole(Role.USER);
		user.setStatus(UserStatus.PENDING_VERIFICATION);
		return user;
	}

	private static Wallet emptyWallet(Long userId) {
		Wallet wallet = new Wallet();
		wallet.setUserId(userId);
		wallet.setCurrency(Wallet.DEFAULT_CURRENCY);
		wallet.setCashBalance(BigDecimal.ZERO);
		wallet.setReservedBalance(BigDecimal.ZERO);
		return wallet;
	}
}
