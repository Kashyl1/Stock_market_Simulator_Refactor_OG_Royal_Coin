package com.tradingsimulator.backend.web;

import java.util.List;
import java.util.function.Function;

import org.springframework.data.domain.Page;

public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

	public static final String FIRST_PAGE = "0";
	public static final String DEFAULT_SIZE = "20";
	public static final int MAX_SIZE = 100;

	public static <E, T> PageResponse<T> of(Page<E> page, Function<E, T> mapper) {
		List<T> content = page.getContent().stream().map(mapper).toList();
		return new PageResponse<>(content, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
	}
}
