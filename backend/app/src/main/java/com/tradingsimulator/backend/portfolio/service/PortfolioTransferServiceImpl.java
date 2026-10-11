package com.tradingsimulator.backend.portfolio.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tradingsimulator.backend.common.error.AppException;
import com.tradingsimulator.backend.currency.Currency;
import com.tradingsimulator.backend.currency.CurrencyRepository;
import com.tradingsimulator.backend.currency.FeeProperties;
import com.tradingsimulator.backend.currency.FxProperties;
import com.tradingsimulator.backend.currency.service.FxConversionService;
import com.tradingsimulator.backend.portfolio.ConversionTerms;
import com.tradingsimulator.backend.portfolio.Portfolio;
import com.tradingsimulator.backend.portfolio.PortfolioError;
import com.tradingsimulator.backend.portfolio.PortfolioRepository;
import com.tradingsimulator.backend.portfolio.PortfolioTransfer;
import com.tradingsimulator.backend.portfolio.PortfolioTransferRepository;
import com.tradingsimulator.backend.portfolio.TransferDirection;
import com.tradingsimulator.backend.portfolio.TransferQuote;
import com.tradingsimulator.backend.wallet.Wallet;
import com.tradingsimulator.backend.wallet.WalletEntryReference;
import com.tradingsimulator.backend.wallet.WalletEntryRepository;
import com.tradingsimulator.backend.wallet.WalletEntryType;
import com.tradingsimulator.backend.wallet.WalletError;
import com.tradingsimulator.backend.wallet.WalletRepository;
import com.tradingsimulator.backend.web.PageResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PortfolioTransferServiceImpl implements PortfolioTransferService {

	private static final String TRANSFERRED_AT = "transferredAt";
	private static final String ID = "id";
	private static final Sort NEWEST_FIRST = Sort.by(Sort.Direction.DESC, TRANSFERRED_AT, ID);

	private final PortfolioRepository portfolios;
	private final PortfolioTransferRepository transfers;
	private final WalletRepository wallets;
	private final WalletEntryRepository entries;
	private final CurrencyRepository currencies;
	private final FxConversionService fx;
	private final FeeProperties fees;
	private final FxProperties fxProperties;
	private final Clock clock;

	@Override
	@Transactional(readOnly = true)
	public TransferQuoteView quote(Long userId, Long portfolioId, TransferDirection direction, BigDecimal amount) {
		Portfolio portfolio = portfolios.ownedBy(userId, portfolioId);
		Wallet mainAccount = wallets.mainAccountOf(userId);
		Wallet portfolioWallet = wallets.ofPortfolio(portfolio.getId());
		return TransferQuoteView.of(quoteFor(mainAccount, portfolioWallet, direction, amount), portfolioWallet.getCurrency(), mainAccount.getCurrency());
	}

	@Override
	@Transactional
	public TransferView transfer(Long userId, Long portfolioId, TransferDirection direction, BigDecimal amount) {
		Portfolio portfolio = portfolios.ownedBy(userId, portfolioId);
		LockedWallets locked = lock(userId, portfolio.getId());
		TransferQuote quote = quoteFor(locked.main(), locked.portfolio(), direction, amount);
		Instant now = clock.instant();
		PortfolioTransfer transfer = transfers.save(PortfolioTransfer.of(portfolio.getId(), quote, now));
		WalletEntryReference reference = WalletEntryReference.portfolioTransfer(transfer.getId());
		entries.save(locked.source(direction).debit(WalletEntryType.TRANSFER_OUT, quote.leaving(), reference, now));
		entries.save(locked.target(direction).credit(WalletEntryType.TRANSFER_IN, quote.arriving(), reference, now));
		return TransferView.of(transfer, locked.portfolio().getCurrency(), locked.main().getCurrency());
	}

	@Override
	@Transactional(readOnly = true)
	public PageResponse<TransferView> transfers(Long userId, Long portfolioId, int page, int size) {
		Portfolio portfolio = portfolios.ownedBy(userId, portfolioId);
		String currency = wallets.ofPortfolio(portfolio.getId()).getCurrency();
		String mainCurrency = wallets.mainAccountOf(userId).getCurrency();
		Page<PortfolioTransfer> found = transfers.findByPortfolioId(portfolio.getId(), PageRequest.of(page, size, NEWEST_FIRST));
		return PageResponse.of(found, transfer -> TransferView.of(transfer, currency, mainCurrency));
	}

	private LockedWallets lock(Long userId, Long portfolioId) {
		List<Wallet> locked = wallets.lockMainAccountAndPortfolio(userId, portfolioId);
		Wallet main = locked.stream().filter(Wallet::isMainAccount).findFirst().orElseThrow(() -> new AppException(WalletError.MAIN_ACCOUNT_MISSING, userId));
		Wallet portfolio = locked.stream().filter(wallet -> portfolioId.equals(wallet.getPortfolioId())).findFirst()
				.orElseThrow(() -> new AppException(WalletError.PORTFOLIO_WALLET_MISSING, portfolioId));
		return new LockedWallets(main, portfolio);
	}

	private TransferQuote quoteFor(Wallet mainAccount, Wallet portfolioWallet, TransferDirection direction, BigDecimal entered) {
		Currency main = currencies.supported(mainAccount.getCurrency());
		Currency portfolio = currencies.supported(portfolioWallet.getCurrency());
		Currency enteredIn = direction == TransferDirection.IN ? main : portfolio;
		if (entered.stripTrailingZeros().scale() > enteredIn.getMinorUnits()) {
			throw new AppException(PortfolioError.AMOUNT_TOO_PRECISE, entered, enteredIn.getCode());
		}
		TransferQuote quote = TransferQuote.of(direction, entered, termsBetween(main, portfolio));
		if (quote.arrivesAsNothing()) {
			throw new AppException(PortfolioError.TRANSFER_TOO_SMALL, entered, enteredIn.getCode());
		}
		return quote;
	}

	private ConversionTerms termsBetween(Currency main, Currency portfolio) {
		if (main.getCode().equals(portfolio.getCode())) {
			return ConversionTerms.sameCurrency(main);
		}
		LocalDate today = LocalDate.now(clock.withZone(fxProperties.zone()));
		return new ConversionTerms(main, portfolio, fx.rate(main.getCode(), portfolio.getCode(), today), fees.fxConversionRate());
	}

	private record LockedWallets(Wallet main, Wallet portfolio) {

		Wallet source(TransferDirection direction) {
			return direction == TransferDirection.IN ? main : portfolio;
		}

		Wallet target(TransferDirection direction) {
			return direction == TransferDirection.IN ? portfolio : main;
		}
	}
}
