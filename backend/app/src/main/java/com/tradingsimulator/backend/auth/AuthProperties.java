package com.tradingsimulator.backend.auth;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import com.tradingsimulator.backend.common.Masking;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Validated
@ConfigurationProperties(prefix = AuthProperties.PREFIX)
public record AuthProperties(
		@NotNull Duration accessTokenTtl,
		@NotNull Duration refreshTokenTtl,
		@NotNull RSAPrivateKey rsaPrivateKey,
		@NotNull RSAPublicKey rsaPublicKey,
		@NotNull Duration verificationTokenTtl,
		@NotNull Duration resetTokenTtl,
		@NotBlank String frontendBaseUrl) {

	public static final String PREFIX = "app.auth";
	public static final String RSA_PRIVATE_KEY = PREFIX + ".rsa-private-key";
	public static final String RSA_PUBLIC_KEY = PREFIX + ".rsa-public-key";

	@Override
	public String toString() {
		return "AuthProperties[accessTokenTtl=" + accessTokenTtl + ", refreshTokenTtl=" + refreshTokenTtl
				+ ", rsaPrivateKey=" + Masking.MASK + ", rsaPublicKey=" + Masking.MASK + ", verificationTokenTtl="
				+ verificationTokenTtl + ", resetTokenTtl=" + resetTokenTtl + ", frontendBaseUrl=" + frontendBaseUrl + "]";
	}
}
