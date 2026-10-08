package com.tradingsimulator.backend.batch;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BatchTypeRepository extends JpaRepository<BatchType, Long> {

	Optional<BatchType> findByCode(BatchTypeCode code);
}
