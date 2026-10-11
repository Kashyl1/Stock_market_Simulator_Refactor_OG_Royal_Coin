package com.tradingsimulator.backend.portfolio;

import com.tradingsimulator.backend.common.persistence.AbstractEntity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

@MappedSuperclass
@Getter
@Setter
public class PortfolioJpa extends AbstractEntity {

	public static final int NAME_MAX_LENGTH = 100;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(nullable = false, length = NAME_MAX_LENGTH)
	private String name;

	@Column(nullable = false)
	private boolean archived;
}
