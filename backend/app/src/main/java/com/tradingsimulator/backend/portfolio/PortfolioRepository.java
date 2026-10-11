package com.tradingsimulator.backend.portfolio;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.tradingsimulator.backend.common.error.AppException;

public interface PortfolioRepository extends JpaRepository<Portfolio, Long> {

	Optional<Portfolio> findByIdAndUserId(Long id, Long userId);

	List<Portfolio> findByUserIdOrderByIdAsc(Long userId);

	@Query("select case when count(p) > 0 then true else false end from Portfolio p where p.userId = :userId and lower(p.name) = lower(:name)")
	boolean existsByUserIdAndNameIgnoreCase(Long userId, String name);

	default Portfolio ownedBy(Long userId, Long portfolioId) {
		return findByIdAndUserId(portfolioId, userId).orElseThrow(() -> new AppException(PortfolioError.NOT_FOUND, portfolioId));
	}
}
