package com.tradingsimulator.backend.account.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tradingsimulator.backend.portfolio.Performance;
import com.tradingsimulator.backend.portfolio.Portfolio;
import com.tradingsimulator.backend.portfolio.PortfolioRepository;
import com.tradingsimulator.backend.portfolio.PortfolioTransfer;
import com.tradingsimulator.backend.portfolio.PortfolioTransferRepository;
import com.tradingsimulator.backend.portfolio.service.PortfolioService;
import com.tradingsimulator.backend.portfolio.service.PortfolioView;
import com.tradingsimulator.backend.wallet.Wallet;
import com.tradingsimulator.backend.wallet.WalletEntry;
import com.tradingsimulator.backend.wallet.WalletEntryReferenceType;
import com.tradingsimulator.backend.wallet.WalletEntryRepository;
import com.tradingsimulator.backend.wallet.WalletEntryType;
import com.tradingsimulator.backend.wallet.WalletRepository;
import com.tradingsimulator.backend.web.PageResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

	private static final String EFFECTIVE_AT = "effectiveAt";
	private static final String ID = "id";
	private static final Sort NEWEST_FIRST = Sort.by(Sort.Direction.DESC, EFFECTIVE_AT, ID);

	private final WalletRepository wallets;
	private final WalletEntryRepository entries;
	private final PortfolioRepository portfolios;
	private final PortfolioTransferRepository transfers;
	private final PortfolioService portfolioService;

	@Override
	@Transactional(readOnly = true)
	public AccountView account(Long userId) {
		Wallet mainAccount = wallets.mainAccountOf(userId);
		BigDecimal value = valueOf(mainAccount, portfolioService.portfolios(userId));
		BigDecimal granted = entries.sumOfType(mainAccount.getId(), WalletEntryType.DEPOSIT);
		Performance performance = Performance.of(mainAccount.getCurrency(), value, granted, BigDecimal.ZERO);
		return new AccountView(mainAccount.getCurrency(), mainAccount.getCashBalance(), mainAccount.available(), performance);
	}

	@Override
	@Transactional(readOnly = true)
	public PageResponse<AccountEntryView> entries(Long userId, int page, int size) {
		Wallet mainAccount = wallets.mainAccountOf(userId);
		Page<WalletEntry> found = entries.findByWalletId(mainAccount.getId(), PageRequest.of(page, size, NEWEST_FIRST));
		Map<Long, Portfolio> portfoliosByTransfer = portfoliosByTransfer(found.getContent());
		return PageResponse.of(found, entry -> AccountEntryView.of(entry, mainAccount.getCurrency(), portfoliosByTransfer.get(entry.getReferenceId())));
	}

	private Map<Long, Portfolio> portfoliosByTransfer(List<WalletEntry> found) {
		Set<Long> transferIds = found.stream().filter(entry -> entry.getReferenceType() == WalletEntryReferenceType.PORTFOLIO_TRANSFER)
				.map(WalletEntry::getReferenceId).collect(Collectors.toUnmodifiableSet());
		List<PortfolioTransfer> referenced = transfers.findAllById(transferIds);
		Set<Long> portfolioIds = referenced.stream().map(PortfolioTransfer::getPortfolioId).collect(Collectors.toUnmodifiableSet());
		Map<Long, Portfolio> byId = portfolios.findAllById(portfolioIds).stream().collect(Collectors.toMap(Portfolio::getId, Function.identity()));
		return referenced.stream().collect(Collectors.toMap(PortfolioTransfer::getId, transfer -> byId.get(transfer.getPortfolioId())));
	}

	private static BigDecimal valueOf(Wallet mainAccount, List<PortfolioView> owned) {
		List<BigDecimal> values = owned.stream().map(portfolio -> portfolio.performance().value()).toList();
		if (values.stream().anyMatch(Objects::isNull)) {
			return null;
		}
		return values.stream().reduce(mainAccount.getCashBalance(), BigDecimal::add);
	}
}
