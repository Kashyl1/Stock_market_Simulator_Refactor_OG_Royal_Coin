package com.tradingsimulator.processengine.store.jpa;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessInstanceRepository extends JpaRepository<ProcessInstanceEntity, UUID> {
}
