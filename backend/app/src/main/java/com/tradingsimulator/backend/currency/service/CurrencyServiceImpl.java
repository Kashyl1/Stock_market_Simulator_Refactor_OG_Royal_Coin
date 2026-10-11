package com.tradingsimulator.backend.currency.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tradingsimulator.backend.currency.CurrencyRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CurrencyServiceImpl implements CurrencyService {

	private final CurrencyRepository currencies;

	@Override
	@Transactional(readOnly = true)
	public List<CurrencyView> activeCurrencies() {
		return currencies.findByActiveTrueOrderByCode().stream().map(CurrencyView::of).toList();
	}
}
