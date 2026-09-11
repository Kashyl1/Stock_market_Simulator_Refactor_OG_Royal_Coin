package com.tradingsimulator.backend.auth.security;

import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Service;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.tradingsimulator.backend.auth.AuthProperties;
import com.tradingsimulator.backend.auth.Role;

@Service
public class JwtServiceImpl implements JwtService {

	static final String ISSUER = "trading-simulator";
	static final String AUDIENCE = "trading-simulator-api";
	static final String CLAIM_EMAIL = "email";
	static final String CLAIM_ROLE = "role";

	private static final Pattern NUMERIC_SUBJECT = Pattern.compile("\\d{1,18}");
	private static final Set<String> ROLE_NAMES = Arrays.stream(Role.values())
			.map(Role::name)
			.collect(Collectors.toUnmodifiableSet());

	private final AuthProperties properties;
	private final Clock clock;
	private final JwtEncoder encoder;
	private final JwtDecoder decoder;

	public JwtServiceImpl(AuthProperties properties, Clock clock) {
		this.properties = properties;
		this.clock = clock;
		this.encoder = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(signingKey(properties))));
		this.decoder = decoder(properties, clock);
	}

	@Override
	public String issueAccessToken(long userId, String email, Role role) {
		Instant now = clock.instant();
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.issuer(ISSUER)
				.audience(List.of(AUDIENCE))
				.subject(Long.toString(userId))
				.issuedAt(now)
				.expiresAt(now.plus(properties.accessTokenTtl()))
				.id(UUID.randomUUID().toString())
				.claim(CLAIM_EMAIL, email)
				.claim(CLAIM_ROLE, role.name())
				.build();
		JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).build();
		return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
	}

	@Override
	public ParsedAccessToken verify(String token) {
		Jwt jwt = decoder.decode(token);
		return new ParsedAccessToken(
				Long.parseLong(jwt.getSubject()),
				jwt.getClaimAsString(CLAIM_EMAIL),
				Role.valueOf(jwt.getClaimAsString(CLAIM_ROLE)));
	}

	private static RSAKey signingKey(AuthProperties properties) {
		try {
			return new RSAKey.Builder(properties.rsaPublicKey())
					.privateKey(properties.rsaPrivateKey())
					.keyIDFromThumbprint()
					.build();
		}
		catch (JOSEException invalidKey) {
			throw new IllegalStateException(invalidKey);
		}
	}

	private static JwtDecoder decoder(AuthProperties properties, Clock clock) {
		NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(properties.rsaPublicKey())
				.signatureAlgorithm(SignatureAlgorithm.RS256)
				.build();
		JwtTimestampValidator timestamps = new JwtTimestampValidator();
		timestamps.setClock(clock);
		decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
				timestamps,
				new JwtIssuerValidator(ISSUER),
				new JwtClaimValidator<List<String>>(JwtClaimNames.AUD,
						audience -> audience != null && audience.contains(AUDIENCE)),
				new JwtClaimValidator<String>(JwtClaimNames.SUB,
						subject -> subject != null && NUMERIC_SUBJECT.matcher(subject).matches()),
				new JwtClaimValidator<String>(CLAIM_ROLE, role -> role != null && ROLE_NAMES.contains(role))));
		return decoder;
	}
}
