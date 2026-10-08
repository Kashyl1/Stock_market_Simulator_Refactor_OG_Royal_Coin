package com.tradingsimulator.backend.currency;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CurrencyRepository extends JpaRepository<Currency, Long> {

	Optional<Currency> findByCodeAndActiveTrue(String code);

	List<Currency> findByActiveTrueOrderByCode();
}
