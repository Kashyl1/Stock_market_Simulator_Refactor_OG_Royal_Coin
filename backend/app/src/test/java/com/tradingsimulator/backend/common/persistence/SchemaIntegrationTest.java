package com.tradingsimulator.backend.common.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.tradingsimulator.backend.auth.User;
import com.tradingsimulator.backend.auth.UserRepository;
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
	void migratedSchemaMatchesTheEntitiesAndStoresTheEmailInLowerCase() {
		User saved = users.save(User.pending(TestUsers.EMAIL_IN_CAPITALS, TestUsers.PASSWORD_HASH, TestUsers.DISPLAY_NAME));
		wallets.save(Wallet.empty(saved.getId()));

		assertThat(saved.getEmail()).isEqualTo(TestUsers.EMAIL);
		assertThat(users.findByEmailIgnoreCase(TestUsers.EMAIL_IN_CAPITALS)).contains(saved);
		assertThat(users.existsByEmailIgnoreCase(TestUsers.EMAIL)).isTrue();
		assertThat(wallets.findAll()).singleElement().satisfies(wallet -> assertThat(wallet.getUserId()).isEqualTo(saved.getId()));
	}
}
