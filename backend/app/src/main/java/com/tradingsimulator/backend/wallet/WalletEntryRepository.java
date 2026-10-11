package com.tradingsimulator.backend.wallet;

import java.math.BigDecimal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface WalletEntryRepository extends JpaRepository<WalletEntry, Long> {

	Page<WalletEntry> findByWalletId(Long walletId, Pageable pageable);

	boolean existsByWalletIdAndReferenceType(Long walletId, WalletEntryReferenceType referenceType);

	@Query("select coalesce(sum(e.amount), 0) from WalletEntry e where e.walletId = :walletId and e.entryType = :entryType")
	BigDecimal sumOfType(Long walletId, WalletEntryType entryType);
}
