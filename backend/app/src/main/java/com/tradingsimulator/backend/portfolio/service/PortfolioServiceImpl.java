package com.tradingsimulator.backend.portfolio.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tradingsimulator.backend.common.error.AppException;
import com.tradingsimulator.backend.currency.Currency;
import com.tradingsimulator.backend.currency.CurrencyRepository;
import com.tradingsimulator.backend.currency.FxProperties;
import com.tradingsimulator.backend.currency.service.FxConversionService;
import com.tradingsimulator.backend.portfolio.Performance;
import com.tradingsimulator.backend.portfolio.Portfolio;
import com.tradingsimulator.backend.portfolio.PortfolioError;
import com.tradingsimulator.backend.portfolio.PortfolioRepository;
import com.tradingsimulator.backend.portfolio.PortfolioTransferRepository;
import com.tradingsimulator.backend.portfolio.TransferSums;
import com.tradingsimulator.backend.portfolio.TransferTotals;
import com.tradingsimulator.backend.wallet.Wallet;
import com.tradingsimulator.backend.wallet.WalletError;
import com.tradingsimulator.backend.wallet.WalletRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PortfolioServiceImpl implements PortfolioService {

	private final PortfolioRepository portfolios;
	private final WalletRepository wallets;
	private final PortfolioTransferRepository transfers;
	private final CurrencyRepository currencies;
	private final FxConversionService fx;
	private final FxProperties fxProperties;
	private final Clock clock;

	@Override
	@Transactional(readOnly = true)
	public List<PortfolioView> portfolios(Long userId) {
		return viewsOf(userId, portfolios.findByUserIdOrderByIdAsc(userId));
	}

	@Override
	@Transactional(readOnly = true)
	public PortfolioView portfolio(Long userId, Long portfolioId) {
		return viewsOf(userId, List.of(portfolios.ownedBy(userId, portfolioId))).getFirst();
	}

	@Override
	@Transactional
	public PortfolioView create(Long userId, String name, String currency) {
		Currency chosen = currencies.supported(currency);
		Portfolio named = Portfolio.named(userId, name);
		if (portfolios.existsByUserIdAndNameIgnoreCase(userId, named.getName())) {
			throw new AppException(PortfolioError.NAME_TAKEN, named.getName());
		}
		Portfolio saved = save(named);
		wallets.save(Wallet.ofPortfolio(userId, saved.getId(), chosen.getCode()));
		return viewsOf(userId, List.of(saved)).getFirst();
	}

	private Portfolio save(Portfolio portfolio) {
		try {
			return portfolios.saveAndFlush(portfolio);
		}
		catch (DataIntegrityViolationException takenName) {
			throw new AppException(PortfolioError.NAME_TAKEN, takenName, portfolio.getName());
		}
	}

	private List<PortfolioView> viewsOf(Long userId, List<Portfolio> owned) {
		if (owned.isEmpty()) {
			return List.of();
		}
		List<Long> ids = owned.stream().map(Portfolio::getId).toList();
		Map<Long, Wallet> walletsByPortfolio = wallets.findByPortfolioIdIn(ids).stream().collect(Collectors.toMap(Wallet::getPortfolioId, Function.identity()));
		Map<Long, List<TransferTotals>> totalsByPortfolio = transfers.totalsOf(ids).stream().collect(Collectors.groupingBy(TransferTotals::portfolioId));
		Currency main = currencies.supported(wallets.mainAccountOf(userId).getCurrency());
		LocalDate today = LocalDate.now(clock.withZone(fxProperties.zone()));
		return owned.stream().map(portfolio -> {
			Wallet wallet = walletOf(walletsByPortfolio, portfolio);
			TransferSums sums = TransferSums.of(totalsByPortfolio.getOrDefault(portfolio.getId(), List.of()));
			Performance performance = Performance.of(main.getCode(), valueIn(main, wallet, today), sums.putIn(), sums.takenOut());
			return PortfolioView.of(portfolio, wallet, performance);
		}).toList();
	}

	private BigDecimal valueIn(Currency main, Wallet wallet, LocalDate day) {
		return fx.findRate(wallet.getCurrency(), main.getCode(), day).map(rate -> main.round(wallet.getCashBalance().multiply(rate))).orElse(null);
	}

	private static Wallet walletOf(Map<Long, Wallet> walletsByPortfolio, Portfolio portfolio) {
		Wallet wallet = walletsByPortfolio.get(portfolio.getId());
		if (wallet == null) {
			throw new AppException(WalletError.PORTFOLIO_WALLET_MISSING, portfolio.getId());
		}
		return wallet;
	}
}
