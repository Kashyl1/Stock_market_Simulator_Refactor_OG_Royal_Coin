package com.tradingsimulator.backend.batch;

import com.tradingsimulator.backend.common.persistence.AbstractEntity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

@MappedSuperclass
@Getter
@Setter
public class BatchEngineJpa extends AbstractEntity {

	@Column(nullable = false)
	private boolean paused;
}
