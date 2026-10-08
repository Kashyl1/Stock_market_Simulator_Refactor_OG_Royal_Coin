package com.tradingsimulator.backend.currency;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.tradingsimulator.backend.common.persistence.AbstractEntity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

@MappedSuperclass
@Getter
@Setter
public class CurrencyJpa extends AbstractEntity {

	public static final int CODE_LENGTH = 3;

	private static final int NAME_MAX_LENGTH = 100;

	@Column(nullable = false, length = CODE_LENGTH)
	private String code;

	@Column(nullable = false, length = NAME_MAX_LENGTH)
	private String name;

	@JdbcTypeCode(SqlTypes.SMALLINT)
	@Column(name = "minor_units", nullable = false)
	private int minorUnits;

	@Column(nullable = false)
	private boolean active;
}
