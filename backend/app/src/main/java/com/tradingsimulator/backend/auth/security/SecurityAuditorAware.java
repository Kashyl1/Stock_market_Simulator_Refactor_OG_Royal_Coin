package com.tradingsimulator.backend.auth.security;

import java.util.Optional;

import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.tradingsimulator.backend.common.persistence.JpaAuditingConfig;

@Component
public class SecurityAuditorAware implements AuditorAware<String> {

	private static final String USER_AUDITOR_PREFIX = "user:";

	@Override
	public Optional<String> getCurrentAuditor() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication != null && authentication.isAuthenticated()
				&& authentication.getPrincipal() instanceof ParsedAccessToken principal) {
			return Optional.of(USER_AUDITOR_PREFIX + principal.userId());
		}
		return Optional.of(JpaAuditingConfig.SYSTEM_AUDITOR);
	}
}
