package com.tradingsimulator.backend.currency;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface FxRateRepository extends JpaRepository<FxRate, Long> {

	@Query("select r from FxRate r where r.baseCurrency = :baseCurrency and r.quoteCurrency = :quoteCurrency and r.rateDate = :rateDate")
	Optional<FxRate> findForPairAndDay(String baseCurrency, String quoteCurrency, LocalDate rateDate);

	Optional<FxRate> findFirstByBaseCurrencyAndQuoteCurrencyAndRateDateLessThanEqualOrderByRateDateDesc(String baseCurrency, String quoteCurrency,
			LocalDate day);

	default Optional<FxRate> findLatestOnOrBefore(String baseCurrency, String quoteCurrency, LocalDate day) {
		return findFirstByBaseCurrencyAndQuoteCurrencyAndRateDateLessThanEqualOrderByRateDateDesc(baseCurrency, quoteCurrency, day);
	}

	@Query("select max(r.rateDate) from FxRate r")
	Optional<LocalDate> findLatestRateDate();
}
