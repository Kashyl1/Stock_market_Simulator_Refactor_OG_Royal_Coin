package com.tradingsimulator.backend.auth.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.security.KeyPair;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.function.UnaryOperator;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwtException;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.tradingsimulator.backend.auth.AuthProperties;
import com.tradingsimulator.backend.auth.Role;
import com.tradingsimulator.backend.support.TestJwtKeys;

class JwtServiceTest {

	private static final Instant NOW = Instant.parse("2026-09-10T12:00:00Z");
	private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
	private static final Duration ACCESS_TOKEN_TTL = Duration.ofMinutes(60);
	private static final Duration REFRESH_TOKEN_TTL = Duration.ofDays(14);
	private static final Duration VERIFICATION_TOKEN_TTL = Duration.ofHours(24);
	private static final Duration RESET_TOKEN_TTL = Duration.ofHours(1);
	private static final String FRONTEND_BASE_URL = "http://localhost:4200";
	private static final long ADMIN_ID = 42L;
	private static final String ADMIN_EMAIL = "ada@example.com";
	private static final long USER_ID = 1L;
	private static final String USER_EMAIL = "x@example.com";
	private static final String FORGED_SIGNATURE_TAIL = "AAAA";
	private static final String FOREIGN_AUDIENCE = "another-api";
	private static final String UNKNOWN_ROLE = "ROOT";
	private static final String NON_NUMERIC_SUBJECT = "ada";
	private static final int TTLS_IN_THE_PAST = 2;

	private static KeyPair rsa;
	private static KeyPair otherRsa;

	@BeforeAll
	static void generateKeys() {
		rsa = TestJwtKeys.newKeyPair();
		otherRsa = TestJwtKeys.newKeyPair();
	}

	@Test
	void issuesAndVerifiesRoundTrip() {
		JwtService service = service(rsa, CLOCK);

		String token = service.issueAccessToken(ADMIN_ID, ADMIN_EMAIL, Role.ADMIN);
		ParsedAccessToken parsed = service.verify(token);

		assertThat(parsed.userId()).isEqualTo(ADMIN_ID);
		assertThat(parsed.email()).isEqualTo(ADMIN_EMAIL);
		assertThat(parsed.role()).isEqualTo(Role.ADMIN);
	}

	@Test
	void rejectsATamperedToken() {
		JwtService service = service(rsa, CLOCK);
		String token = service.issueAccessToken(USER_ID, USER_EMAIL, Role.USER);
		String tampered = token.substring(0, token.length() - FORGED_SIGNATURE_TAIL.length()) + FORGED_SIGNATURE_TAIL;

		assertThatThrownBy(() -> service.verify(tampered)).isInstanceOf(JwtException.class);
	}

	@Test
	void rejectsAnExpiredToken() {
		Clock issuedLongAgo = Clock.offset(CLOCK, ACCESS_TOKEN_TTL.multipliedBy(TTLS_IN_THE_PAST).negated());
		String token = service(rsa, issuedLongAgo).issueAccessToken(USER_ID, USER_EMAIL, Role.USER);

		assertThatThrownBy(() -> service(rsa, CLOCK).verify(token)).isInstanceOf(JwtException.class);
	}

	@Test
	void rejectsATokenSignedWithAnotherKey() {
		String token = service(otherRsa, CLOCK).issueAccessToken(USER_ID, USER_EMAIL, Role.USER);

		assertThatThrownBy(() -> service(rsa, CLOCK).verify(token)).isInstanceOf(JwtException.class);
	}

	@Test
	void rejectsATokenForAnotherAudience() throws Exception {
		String token = signedToken(claims -> claims.audience(FOREIGN_AUDIENCE));

		assertThatThrownBy(() -> service(rsa, CLOCK).verify(token)).isInstanceOf(JwtException.class);
	}

	@Test
	void rejectsATokenWithAnUnknownRole() throws Exception {
		String token = signedToken(claims -> claims.claim(JwtServiceImpl.CLAIM_ROLE, UNKNOWN_ROLE));

		assertThatThrownBy(() -> service(rsa, CLOCK).verify(token)).isInstanceOf(JwtException.class);
	}

	@Test
	void rejectsATokenWithANonNumericSubject() throws Exception {
		String token = signedToken(claims -> claims.subject(NON_NUMERIC_SUBJECT));

		assertThatThrownBy(() -> service(rsa, CLOCK).verify(token)).isInstanceOf(JwtException.class);
	}

	private static JwtService service(KeyPair keyPair, Clock clock) {
		AuthProperties properties = new AuthProperties(
				ACCESS_TOKEN_TTL,
				REFRESH_TOKEN_TTL,
				(RSAPrivateKey) keyPair.getPrivate(),
				(RSAPublicKey) keyPair.getPublic(),
				VERIFICATION_TOKEN_TTL,
				RESET_TOKEN_TTL,
				FRONTEND_BASE_URL);
		return new JwtServiceImpl(properties, clock);
	}

	private static String signedToken(UnaryOperator<JWTClaimsSet.Builder> override) throws Exception {
		JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder()
				.issuer(JwtServiceImpl.ISSUER)
				.audience(JwtServiceImpl.AUDIENCE)
				.subject(Long.toString(USER_ID))
				.issueTime(Date.from(NOW))
				.expirationTime(Date.from(NOW.plus(ACCESS_TOKEN_TTL)))
				.claim(JwtServiceImpl.CLAIM_EMAIL, USER_EMAIL)
				.claim(JwtServiceImpl.CLAIM_ROLE, Role.USER.name());
		SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), override.apply(claims).build());
		jwt.sign(new RSASSASigner(rsa.getPrivate()));
		return jwt.serialize();
	}
}
