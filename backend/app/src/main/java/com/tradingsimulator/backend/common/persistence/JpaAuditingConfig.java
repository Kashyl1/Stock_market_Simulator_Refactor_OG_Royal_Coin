package com.tradingsimulator.backend.common.persistence;

import java.util.Optional;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {

	public static final String SYSTEM_AUDITOR = "system";

	@Bean
	AuditorAware<String> auditorAware() {
		return () -> Optional.of(SYSTEM_AUDITOR);
	}
}
