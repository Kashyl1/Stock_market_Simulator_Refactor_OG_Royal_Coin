package com.tradingsimulator.backend.wallet;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.tradingsimulator.backend.auth.Role;
import com.tradingsimulator.backend.auth.UserStatus;
import com.tradingsimulator.backend.common.persistence.JpaAuditingConfig;
import com.tradingsimulator.backend.support.TestDatabase;
import com.tradingsimulator.backend.support.TestFx;
import com.tradingsimulator.backend.support.TestMoney;
import com.tradingsimulator.backend.support.TestTags;
import com.tradingsimulator.backend.support.TestUsers;

@Tag(TestTags.INTEGRATION)
@Testcontainers
class MainAccountBackfillIntegrationTest {

	private static final String BEFORE_PORTFOLIO_CASH = "14";
	private static final String LATEST = "latest";
	private static final String ACTIVE_EMAIL = "active.troodon@example.com";
	private static final String PENDING_EMAIL = "pending.troodon@example.com";
	private static final String TRADER_EMAIL = "trader.troodon@example.com";
	private static final BigDecimal TRADED_DOLLARS = new BigDecimal("50.00");
	private static final String INSERT_USER = "insert into app_user (email, password_hash, display_name, role, status) values (?, ?, ?, ?, ?) returning id";
	private static final String INSERT_WALLET = "insert into wallet (user_id, currency, cash_balance) values (?, ?, ?) returning id";
	private static final String INSERT_ENTRY = "insert into wallet_entry (wallet_id, entry_type, amount, balance_after) values (?, ?, ?, ?)";
	private static final String WALLET_QUERY = "select currency || ' ' || cash_balance from wallet where id = ?";
	private static final String ENTRY_QUERY = "select entry_type || ' ' || amount || ' ' || balance_after || ' ' || reference_type from wallet_entry"
			+ " where wallet_id = ? order by id";
	private static final String WALLET_AUTHORS_QUERY = "select h_changed_by from wallet_h where id = ?";
	private static final String SEPARATOR = " ";

	@Container
	static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(TestDatabase.POSTGRES_IMAGE);

	@Test
	void existingMainAccountsMoveToZlotyAndActiveOnesReceiveTheirStartingBalance() {
		JdbcTemplate jdbc = new JdbcTemplate(new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()));
		migrateTo(BEFORE_PORTFOLIO_CASH);
		long activeWallet = walletOf(jdbc, userOf(jdbc, ACTIVE_EMAIL, UserStatus.ACTIVE), BigDecimal.ZERO);
		long pendingWallet = walletOf(jdbc, userOf(jdbc, PENDING_EMAIL, UserStatus.PENDING_VERIFICATION), BigDecimal.ZERO);
		long traderWallet = walletOf(jdbc, userOf(jdbc, TRADER_EMAIL, UserStatus.ACTIVE), TRADED_DOLLARS);
		jdbc.update(INSERT_ENTRY, traderWallet, WalletEntryType.DEPOSIT.name(), TRADED_DOLLARS, TRADED_DOLLARS);

		migrateTo(LATEST);

		String startingBalance = TestMoney.STARTING_BALANCE.toPlainString();
		assertThat(jdbc.queryForObject(WALLET_QUERY, String.class, activeWallet)).isEqualTo(TestFx.PLN + SEPARATOR + startingBalance);
		assertThat(jdbc.queryForList(ENTRY_QUERY, String.class, activeWallet)).containsExactly(String.join(SEPARATOR, WalletEntryType.DEPOSIT.name(),
				startingBalance, startingBalance, WalletEntryReferenceType.STARTING_BALANCE.name()));
		assertThat(jdbc.queryForList(WALLET_AUTHORS_QUERY, String.class, activeWallet)).isNotEmpty().containsOnly(JpaAuditingConfig.SYSTEM_AUDITOR);
		assertThat(jdbc.queryForObject(WALLET_QUERY, String.class, pendingWallet)).isEqualTo(TestFx.PLN + SEPARATOR + BigDecimal.ZERO.setScale(TestFx.CENTS));
		assertThat(jdbc.queryForObject(WALLET_QUERY, String.class, traderWallet)).isEqualTo(TestFx.USD + SEPARATOR + TRADED_DOLLARS.toPlainString());
	}

	private static void migrateTo(String version) {
		Flyway.configure().dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()).locations(TestDatabase.MIGRATIONS).target(version)
				.load().migrate();
	}

	private static long userOf(JdbcTemplate jdbc, String email, UserStatus status) {
		return jdbc.queryForObject(INSERT_USER, Long.class, email, TestUsers.PASSWORD_HASH, TestUsers.DISPLAY_NAME, Role.USER.name(), status.name());
	}

	private static long walletOf(JdbcTemplate jdbc, long userId, BigDecimal cashBalance) {
		return jdbc.queryForObject(INSERT_WALLET, Long.class, userId, TestFx.USD, cashBalance);
	}
}
