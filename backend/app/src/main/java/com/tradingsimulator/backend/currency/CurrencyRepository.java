package com.tradingsimulator.backend.currency;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tradingsimulator.backend.common.error.AppException;

public interface CurrencyRepository extends JpaRepository<Currency, Long> {

	Optional<Currency> findByCodeAndActiveTrue(String code);

	List<Currency> findByActiveTrueOrderByCode();

	default Currency supported(String code) {
		return findByCodeAndActiveTrue(code).orElseThrow(() -> new AppException(FxError.UNKNOWN_CURRENCY, code));
	}
}
