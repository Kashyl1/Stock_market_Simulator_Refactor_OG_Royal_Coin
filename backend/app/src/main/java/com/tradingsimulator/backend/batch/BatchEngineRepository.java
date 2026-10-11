package com.tradingsimulator.backend.batch;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tradingsimulator.backend.common.error.AppException;

public interface BatchEngineRepository extends JpaRepository<BatchEngine, Long> {

	Optional<BatchEngine> findFirstByOrderByIdAsc();

	default BatchEngine current() {
		return findFirstByOrderByIdAsc().orElseThrow(() -> new AppException(BatchError.ENGINE_SWITCH_MISSING));
	}
}
