package com.tradingsimulator.processengine.store.jpa;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessStepLogRepository extends JpaRepository<ProcessStepLogEntity, Long> {

	int countByInstanceId(UUID instanceId);

	List<ProcessStepLogEntity> findByInstanceIdOrderBySequenceNoAsc(UUID instanceId);

	Optional<ProcessStepLogEntity> findFirstByInstanceIdAndStepOrderBySequenceNoDesc(UUID instanceId, String step);
}
