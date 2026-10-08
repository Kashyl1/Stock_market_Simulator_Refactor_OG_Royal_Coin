package com.tradingsimulator.backend.currency.provider.nbp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.tradingsimulator.backend.common.error.AppException;
import com.tradingsimulator.backend.currency.FxError;
import com.tradingsimulator.backend.support.TestFx;

class NbpFxRateProviderTest {

	private static final String TABLE = """
			{"table":"A","no":"%s","effectiveDate":"%s","rates":[
			{"currency":"dolar amerykański","code":"%s","mid":%s},
			{"currency":"jen (Japonia)","code":"%s","mid":%s}]}""";
	private static final String WEDNESDAY_TABLE = "195/A/NBP/2026";
	private static final String THURSDAY_TABLE = "196/A/NBP/2026";
	private static final String NO_TABLES = "[]";
	private static final LocalDate NEW_YEAR = LocalDate.of(2026, 1, 1);
	private static final LocalDate END_OF_FIRST_REQUEST = LocalDate.of(2026, 4, 3);
	private static final LocalDate START_OF_SECOND_REQUEST = LocalDate.of(2026, 4, 4);
	private static final LocalDate END_OF_SECOND_REQUEST = LocalDate.of(2026, 7, 5);
	private static final LocalDate START_OF_LAST_REQUEST = LocalDate.of(2026, 7, 6);
	private static final LocalDate TWO_HUNDREDTH_DAY = LocalDate.of(2026, 7, 19);

	private final RestClient.Builder builder = RestClient.builder().baseUrl(TestFx.NBP_BASE_URL);
	private final MockRestServiceServer nbp = MockRestServiceServer.bindTo(builder).build();
	private final NbpFxRateProvider provider = new NbpFxRateProvider(builder.build(), TestFx.properties());

	@Test
	void turnsEveryRateOfEveryTableIntoARateAgainstTheZloty() {
		String tables = "[" + table(WEDNESDAY_TABLE, TestFx.WEDNESDAY) + "," + table(THURSDAY_TABLE, TestFx.THURSDAY) + "]";
		nbp.expect(requestTo(tablesBetween(TestFx.WEDNESDAY, TestFx.THURSDAY))).andRespond(withSuccess(tables, MediaType.APPLICATION_JSON));

		assertThat(provider.ratesBetween(TestFx.WEDNESDAY, TestFx.THURSDAY)).containsExactly(TestFx.published(TestFx.USD, TestFx.WEDNESDAY, TestFx.USD_MID),
				TestFx.published(TestFx.JPY, TestFx.WEDNESDAY, TestFx.JPY_MID), TestFx.published(TestFx.USD, TestFx.THURSDAY, TestFx.USD_MID),
				TestFx.published(TestFx.JPY, TestFx.THURSDAY, TestFx.JPY_MID));
		nbp.verify();
	}

	@Test
	void asksForAtMostTheAllowedNumberOfDaysAtOnceAndTreatsNotFoundAsNoTable() {
		nbp.expect(requestTo(tablesBetween(NEW_YEAR, END_OF_FIRST_REQUEST))).andRespond(withSuccess(NO_TABLES, MediaType.APPLICATION_JSON));
		nbp.expect(requestTo(tablesBetween(START_OF_SECOND_REQUEST, END_OF_SECOND_REQUEST))).andRespond(withSuccess(NO_TABLES, MediaType.APPLICATION_JSON));
		nbp.expect(requestTo(tablesBetween(START_OF_LAST_REQUEST, TWO_HUNDREDTH_DAY))).andRespond(withResourceNotFound());

		assertThat(provider.ratesBetween(NEW_YEAR, TWO_HUNDREDTH_DAY)).isEmpty();
		nbp.verify();
	}

	@Test
	void reportsAnUnavailableNbpAsOurOwnError() {
		nbp.expect(requestTo(tablesBetween(TestFx.THURSDAY, TestFx.THURSDAY))).andRespond(withServerError());

		assertThatThrownBy(() -> provider.ratesBetween(TestFx.THURSDAY, TestFx.THURSDAY)).isInstanceOf(AppException.class)
				.extracting(failure -> ((AppException) failure).errorCode()).isEqualTo(FxError.PROVIDER_UNAVAILABLE);
	}

	private static String table(String number, LocalDate day) {
		return TABLE.formatted(number, day, TestFx.USD, TestFx.USD_MID, TestFx.JPY, TestFx.JPY_MID);
	}

	private static String tablesBetween(LocalDate from, LocalDate to) {
		return TestFx.NBP_BASE_URL + "/exchangerates/tables/A/" + from + "/" + to + "/?format=json";
	}
}
