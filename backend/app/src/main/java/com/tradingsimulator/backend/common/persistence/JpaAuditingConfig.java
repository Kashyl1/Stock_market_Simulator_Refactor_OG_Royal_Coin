package com.tradingsimulator.backend.common.persistence;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {

	public static final String SYSTEM_AUDITOR = "system";
}
