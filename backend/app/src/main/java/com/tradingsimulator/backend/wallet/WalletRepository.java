package com.tradingsimulator.backend.wallet;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import com.tradingsimulator.backend.common.error.AppException;

import jakarta.persistence.LockModeType;

public interface WalletRepository extends JpaRepository<Wallet, Long> {

	Optional<Wallet> findByUserIdAndPortfolioIdIsNull(Long userId);

	Optional<Wallet> findByPortfolioId(Long portfolioId);

	List<Wallet> findByPortfolioIdIn(Collection<Long> portfolioIds);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select w from Wallet w where w.userId = :userId and (w.portfolioId is null or w.portfolioId = :portfolioId) order by w.id")
	List<Wallet> lockMainAccountAndPortfolio(Long userId, Long portfolioId);

	default Wallet mainAccountOf(Long userId) {
		return findByUserIdAndPortfolioIdIsNull(userId).orElseThrow(() -> new AppException(WalletError.MAIN_ACCOUNT_MISSING, userId));
	}

	default Wallet ofPortfolio(Long portfolioId) {
		return findByPortfolioId(portfolioId).orElseThrow(() -> new AppException(WalletError.PORTFOLIO_WALLET_MISSING, portfolioId));
	}
}
