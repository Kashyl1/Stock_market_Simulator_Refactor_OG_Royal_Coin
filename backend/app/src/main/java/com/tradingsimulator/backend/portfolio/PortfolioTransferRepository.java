package com.tradingsimulator.backend.portfolio;

import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PortfolioTransferRepository extends JpaRepository<PortfolioTransfer, Long> {

	Page<PortfolioTransfer> findByPortfolioId(Long portfolioId, Pageable pageable);

	@Query("select new com.tradingsimulator.backend.portfolio.TransferTotals(t.portfolioId, t.direction, sum(t.amount), sum(t.mainAmount))"
			+ " from PortfolioTransfer t where t.portfolioId in :portfolioIds group by t.portfolioId, t.direction")
	List<TransferTotals> totalsOf(Collection<Long> portfolioIds);
}
