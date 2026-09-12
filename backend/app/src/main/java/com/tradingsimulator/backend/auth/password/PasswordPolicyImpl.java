package com.tradingsimulator.backend.auth.password;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import com.tradingsimulator.backend.auth.AuthError;
import com.tradingsimulator.backend.common.error.AppException;

@Service
public class PasswordPolicyImpl implements PasswordPolicy {

	private static final String COMMON_PASSWORDS_RESOURCE = "auth/common-passwords.txt";

	private final Set<String> commonPasswords = loadCommonPasswords();

	@Override
	public void check(String password) {
		if (password.length() < MIN_LENGTH || password.length() > MAX_LENGTH || isCommon(password)) {
			throw new AppException(AuthError.PASSWORD_TOO_WEAK, MIN_LENGTH);
		}
	}

	private boolean isCommon(String password) {
		return commonPasswords.contains(password.toLowerCase(Locale.ROOT));
	}

	private static Set<String> loadCommonPasswords() {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(
				new ClassPathResource(COMMON_PASSWORDS_RESOURCE).getInputStream(), StandardCharsets.UTF_8))) {
			return reader.lines()
					.map(line -> line.trim().toLowerCase(Locale.ROOT))
					.filter(line -> !line.isEmpty())
					.collect(Collectors.toUnmodifiableSet());
		}
		catch (IOException unreadableResource) {
			throw new IllegalStateException(unreadableResource);
		}
	}
}
