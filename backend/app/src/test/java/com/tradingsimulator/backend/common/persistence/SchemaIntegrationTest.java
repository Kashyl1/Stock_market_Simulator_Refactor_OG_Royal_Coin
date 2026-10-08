package com.tradingsimulator.backend.common.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.tradingsimulator.backend.auth.User;
import com.tradingsimulator.backend.auth.UserRepository;
import com.tradingsimulator.backend.auth.UserStatus;
import com.tradingsimulator.backend.currency.Currency;
import com.tradingsimulator.backend.currency.CurrencyRepository;
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
	private static final List<String> TABLES_KEPT_WITHOUT_HISTORY = List.of("flyway_schema_history", "instrument_quote", "price_candle");
	private static final String TABLES_WITHOUT_HISTORY_QUERY = "select relname from pg_class where relkind = 'r' and relnamespace = 'public'::regnamespace"
			+ " and right(relname, 2) <> '_h' and to_regclass(relname || '_h') is null order by relname";
	private static final String HISTORY_DRIFT_QUERY = "select table_name || '.' || column_name from history_column_drift";
	private static final String USER_HISTORY_QUERY = "select h_operation || ':' || status from app_user_h where id = ? order by h_id";
	private static final String INSERT_PROCESS = "insert into process_instance (id, definition_key, current_step, status, context_json, created_at, updated_at)"
			+ " values (?, 'history-check', 'START', 'RUNNING', '{}', now() - interval '1 hour', now() - interval '1 hour')";
	private static final String ADVANCE_PROCESS = "update process_instance set current_step = 'NEXT', updated_at = now(), version = version + 1 where id = ?";
	private static final String DELETE_PROCESS = "delete from process_instance where id = ?";
	private static final String PROCESS_HISTORY_QUERY = "select h_operation || ':' || current_step from process_instance_h where id = ? order by h_id";
	private static final String PROCESS_VALID_TO_QUERY = "select h_valid_to from process_instance_h where id = ?";
	private static final String PROCESS_UPDATED_AT_QUERY = "select updated_at from process_instance where id = ?";
	private static final UUID ADVANCED_PROCESS = UUID.fromString("11111111-1111-1111-1111-111111111111");
	private static final UUID DELETED_PROCESS = UUID.fromString("22222222-2222-2222-2222-222222222222");
	private static final String UPDATED_PENDING_USER = "UPDATE:" + UserStatus.PENDING_VERIFICATION.name();
	private static final String UPDATED_AT_START = "UPDATE:START";
	private static final String DELETED_AT_START = "DELETE:START";
	private static final int SEEDED_CURRENCIES = 30;
	private static final String POLISH_ZLOTY = "PLN";
	private static final String JAPANESE_YEN = "JPY";
	private static final int ZLOTY_MINOR_UNITS = 2;
	private static final int YEN_MINOR_UNITS = 0;
	private static final String UNKNOWN_CURRENCY = "XYZ";
	private static final String WALLET_OWNER_EMAIL = "wallet.troodon@example.com";

	@Container
	@ServiceConnection
	static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(POSTGRES_IMAGE);

	@Autowired
	private UserRepository users;

	@Autowired
	private WalletRepository wallets;

	@Autowired
	private CurrencyRepository currencies;

	@Autowired
	private JdbcTemplate jdbc;

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

	@Test
	void everyTableHasAHistoryTableExceptTheHighFrequencyOnes() {
		assertThat(jdbc.queryForList(TABLES_WITHOUT_HISTORY_QUERY, String.class)).containsExactlyElementsOf(TABLES_KEPT_WITHOUT_HISTORY);
	}

	@Test
	void everyHistoryTableHasAllColumnsOfItsTable() {
		assertThat(jdbc.queryForList(HISTORY_DRIFT_QUERY, String.class)).isEmpty();
	}

	@Test
	void anUpdateThroughJpaKeepsThePreviousVersionUnderTheSameId() {
		User user = users.save(User.pending(TestUsers.OTHER_EMAIL, TestUsers.PASSWORD_HASH, TestUsers.DISPLAY_NAME));
		user.setStatus(UserStatus.ACTIVE);
		users.save(user);

		assertThat(jdbc.queryForList(USER_HISTORY_QUERY, String.class, user.getId())).containsExactly(UPDATED_PENDING_USER);
	}

	@Test
	void aTableWithoutAuditColumnsIsVersionedFromItsUpdatedAt() {
		jdbc.update(INSERT_PROCESS, ADVANCED_PROCESS);
		jdbc.update(ADVANCE_PROCESS, ADVANCED_PROCESS);

		assertThat(jdbc.queryForList(PROCESS_HISTORY_QUERY, String.class, ADVANCED_PROCESS)).containsExactly(UPDATED_AT_START);
		assertThat(jdbc.queryForObject(PROCESS_VALID_TO_QUERY, Timestamp.class, ADVANCED_PROCESS))
				.isEqualTo(jdbc.queryForObject(PROCESS_UPDATED_AT_QUERY, Timestamp.class, ADVANCED_PROCESS));
	}

	@Test
	void aDeleteKeepsTheLastVersion() {
		jdbc.update(INSERT_PROCESS, DELETED_PROCESS);
		jdbc.update(DELETE_PROCESS, DELETED_PROCESS);

		assertThat(jdbc.queryForList(PROCESS_HISTORY_QUERY, String.class, DELETED_PROCESS)).containsExactly(DELETED_AT_START);
	}

	@Test
	void theCurrencyDictionaryIsSeededWithItsMinorUnits() {
		Map<String, Integer> minorUnits = currencies.findAll().stream().collect(Collectors.toMap(Currency::getCode, Currency::getMinorUnits));

		assertThat(minorUnits).hasSize(SEEDED_CURRENCIES).containsEntry(POLISH_ZLOTY, ZLOTY_MINOR_UNITS).containsEntry(JAPANESE_YEN, YEN_MINOR_UNITS);
	}

	@Test
	void aWalletInAnUnknownCurrencyIsRejected() {
		User owner = users.save(User.pending(WALLET_OWNER_EMAIL, TestUsers.PASSWORD_HASH, TestUsers.DISPLAY_NAME));
		Wallet wallet = Wallet.empty(owner.getId());
		wallet.setCurrency(UNKNOWN_CURRENCY);

		assertThatThrownBy(() -> wallets.saveAndFlush(wallet)).isInstanceOf(DataIntegrityViolationException.class);
	}
}
