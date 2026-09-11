package com.tradingsimulator.backend.support;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import org.springframework.test.context.DynamicPropertyRegistry;

import com.tradingsimulator.backend.auth.AuthProperties;

public final class TestJwtKeys {

	private static final String RSA = "RSA";
	private static final int RSA_KEY_SIZE = 2048;
	private static final int PEM_LINE_LENGTH = 64;
	private static final String PEM_LINE_SEPARATOR = "\n";
	private static final String PEM_BOUNDARY = "-----";
	private static final String PEM_BEGIN = PEM_BOUNDARY + "BEGIN ";
	private static final String PEM_END = PEM_BOUNDARY + "END ";
	private static final String PRIVATE_KEY_PEM_TYPE = "PRIVATE KEY";
	private static final String PUBLIC_KEY_PEM_TYPE = "PUBLIC KEY";
	private static final KeyPair KEY_PAIR = newKeyPair();

	private TestJwtKeys() {
	}

	public static void register(DynamicPropertyRegistry registry) {
		registry.add(AuthProperties.RSA_PRIVATE_KEY, () -> pem(PRIVATE_KEY_PEM_TYPE, KEY_PAIR.getPrivate().getEncoded()));
		registry.add(AuthProperties.RSA_PUBLIC_KEY, () -> pem(PUBLIC_KEY_PEM_TYPE, KEY_PAIR.getPublic().getEncoded()));
	}

	public static KeyPair newKeyPair() {
		try {
			KeyPairGenerator generator = KeyPairGenerator.getInstance(RSA);
			generator.initialize(RSA_KEY_SIZE);
			return generator.generateKeyPair();
		}
		catch (NoSuchAlgorithmException unavailable) {
			throw new IllegalStateException(unavailable);
		}
	}

	private static String pem(String type, byte[] der) {
		String body = Base64.getMimeEncoder(PEM_LINE_LENGTH, PEM_LINE_SEPARATOR.getBytes(StandardCharsets.US_ASCII))
				.encodeToString(der);
		return PEM_BEGIN + type + PEM_BOUNDARY + PEM_LINE_SEPARATOR + body + PEM_LINE_SEPARATOR
				+ PEM_END + type + PEM_BOUNDARY + PEM_LINE_SEPARATOR;
	}
}
