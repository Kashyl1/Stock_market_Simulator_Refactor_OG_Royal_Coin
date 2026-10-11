package com.tradingsimulator.backend.currency.web;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tradingsimulator.backend.currency.service.CurrencyService;
import com.tradingsimulator.backend.currency.service.CurrencyView;
import com.tradingsimulator.backend.web.ApiPaths;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(CurrencyController.BASE)
@RequiredArgsConstructor
public class CurrencyController {

	public static final String BASE = ApiPaths.API + "/currencies";

	private final CurrencyService currencies;

	@GetMapping
	public List<CurrencyView> activeCurrencies() {
		return currencies.activeCurrencies();
	}
}
