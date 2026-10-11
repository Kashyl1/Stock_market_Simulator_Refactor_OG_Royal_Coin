package com.tradingsimulator.backend.portfolio.web;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.tradingsimulator.backend.auth.service.CurrentUserService;
import com.tradingsimulator.backend.portfolio.TransferDirection;
import com.tradingsimulator.backend.portfolio.service.PortfolioService;
import com.tradingsimulator.backend.portfolio.service.PortfolioTransferService;
import com.tradingsimulator.backend.portfolio.service.PortfolioView;
import com.tradingsimulator.backend.portfolio.service.TransferQuoteView;
import com.tradingsimulator.backend.portfolio.service.TransferView;
import com.tradingsimulator.backend.portfolio.web.dto.CreatePortfolioRequest;
import com.tradingsimulator.backend.portfolio.web.dto.TransferRequest;
import com.tradingsimulator.backend.web.PageResponse;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(PortfolioPaths.BASE)
@RequiredArgsConstructor
public class PortfolioController {

	private final PortfolioService portfolios;
	private final PortfolioTransferService transfers;
	private final CurrentUserService currentUser;

	@GetMapping
	public List<PortfolioView> portfolios() {
		return portfolios.portfolios(userId());
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public PortfolioView create(@Valid @RequestBody CreatePortfolioRequest request) {
		return portfolios.create(userId(), request.name(), request.currency());
	}

	@GetMapping(PortfolioPaths.PORTFOLIO)
	public PortfolioView portfolio(@PathVariable Long id) {
		return portfolios.portfolio(userId(), id);
	}

	@GetMapping(PortfolioPaths.TRANSFER_QUOTE)
	public TransferQuoteView quote(@PathVariable Long id, @RequestParam TransferDirection direction, @RequestParam @Positive BigDecimal amount) {
		return transfers.quote(userId(), id, direction, amount);
	}

	@PostMapping(PortfolioPaths.TRANSFERS)
	@ResponseStatus(HttpStatus.CREATED)
	public TransferView transfer(@PathVariable Long id, @Valid @RequestBody TransferRequest request) {
		return transfers.transfer(userId(), id, request.direction(), request.amount());
	}

	@GetMapping(PortfolioPaths.TRANSFERS)
	public PageResponse<TransferView> transfers(@PathVariable Long id, @RequestParam(defaultValue = PageResponse.FIRST_PAGE) @PositiveOrZero int page,
			@RequestParam(defaultValue = PageResponse.DEFAULT_SIZE) @Positive @Max(PageResponse.MAX_SIZE) int size) {
		return transfers.transfers(userId(), id, page, size);
	}

	private long userId() {
		return currentUser.current().id();
	}
}
