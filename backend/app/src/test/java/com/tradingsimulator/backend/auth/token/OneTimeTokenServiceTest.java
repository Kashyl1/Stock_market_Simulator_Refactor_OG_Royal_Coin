package com.tradingsimulator.backend.auth.token;

import static org.assertj.core.api.Assertions.assertThat;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import org.junit.jupiter.api.Test;

class OneTimeTokenServiceTest {

	private static final String SEEDED_ALGORITHM = "SHA1PRNG";
	private static final long SEED = 42L;
	private static final String URL_SAFE_TOKEN = "[A-Za-z0-9_-]+";
	private static final String ANY_TOKEN = "any-raw-token";

	private final OneTimeTokenService service = new OneTimeTokenServiceImpl(seededRandom());

	@Test
	void issuesAUrlSafeTokenTogetherWithItsHash() {
		OneTimeToken token = service.issue();

		assertThat(token.raw()).matches(URL_SAFE_TOKEN);
		assertThat(token.hash()).matches(URL_SAFE_TOKEN).isNotEqualTo(token.raw());
		assertThat(service.hash(token.raw())).isEqualTo(token.hash());
	}

	@Test
	void issuesADifferentTokenEveryTime() {
		assertThat(service.issue().raw()).isNotEqualTo(service.issue().raw());
	}

	@Test
	void hashesTheSameTokenToTheSameValue() {
		assertThat(service.hash(ANY_TOKEN)).isEqualTo(service.hash(ANY_TOKEN));
	}

	@Test
	void takesItsRandomnessFromTheInjectedSource() {
		OneTimeToken first = new OneTimeTokenServiceImpl(seededRandom()).issue();
		OneTimeToken second = new OneTimeTokenServiceImpl(seededRandom()).issue();

		assertThat(first).isEqualTo(second);
	}

	private static SecureRandom seededRandom() {
		try {
			SecureRandom random = SecureRandom.getInstance(SEEDED_ALGORITHM);
			random.setSeed(SEED);
			return random;
		}
		catch (NoSuchAlgorithmException unavailable) {
			throw new IllegalStateException(unavailable);
		}
	}
}
