package com.tradingsimulator.backend.common.persistence;

import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class HistoryActor {

	private static final String ACTOR_SETTING = "app.actor";
	private static final String SETTING_PARAMETER = "setting";
	private static final String ACTOR_PARAMETER = "actor";
	private static final String DECLARE_FOR_TRANSACTION = "select set_config(:" + SETTING_PARAMETER + ", :" + ACTOR_PARAMETER + ", true)";

	private final EntityManager entityManager;
	private final AuditorAware<String> auditorAware;

	@Transactional(propagation = Propagation.MANDATORY)
	public void declareCurrentAuditor() {
		String actor = auditorAware.getCurrentAuditor().orElse(JpaAuditingConfig.SYSTEM_AUDITOR);
		Query declaration = entityManager.createNativeQuery(DECLARE_FOR_TRANSACTION);
		declaration.setParameter(SETTING_PARAMETER, ACTOR_SETTING).setParameter(ACTOR_PARAMETER, actor).getSingleResult();
	}
}
