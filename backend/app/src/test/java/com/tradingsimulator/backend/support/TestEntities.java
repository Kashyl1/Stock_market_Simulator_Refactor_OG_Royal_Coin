package com.tradingsimulator.backend.support;

import org.springframework.test.util.ReflectionTestUtils;

public final class TestEntities {

	private static final String ID_FIELD = "id";

	public static <T> T withId(T entity, long id) {
		ReflectionTestUtils.setField(entity, ID_FIELD, id);
		return entity;
	}

	private TestEntities() {
	}
}
