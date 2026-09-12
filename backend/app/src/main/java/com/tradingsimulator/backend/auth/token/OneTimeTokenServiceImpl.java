package com.tradingsimulator.backend.auth.token;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OneTimeTokenServiceImpl implements OneTimeTokenService {

	private static final int TOKEN_BYTES = 32;
	private static final String HASH_ALGORITHM = "SHA-256";

	private final SecureRandom secureRandom;

	@Override
	public OneTimeToken issue() {
		byte[] token = new byte[TOKEN_BYTES];
		secureRandom.nextBytes(token);
		String raw = encode(token);
		return new OneTimeToken(raw, hash(raw));
	}

	@Override
	public String hash(String rawToken) {
		return encode(digest().digest(rawToken.getBytes(StandardCharsets.UTF_8)));
	}

	private static String encode(byte[] bytes) {
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	private static MessageDigest digest() {
		try {
			return MessageDigest.getInstance(HASH_ALGORITHM);
		}
		catch (NoSuchAlgorithmException unavailable) {
			throw new IllegalStateException(unavailable);
		}
	}
}
