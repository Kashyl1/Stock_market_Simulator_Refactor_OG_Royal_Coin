package com.tradingsimulator.backend.batch;

import com.tradingsimulator.backend.common.persistence.AbstractEntity;

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

@MappedSuperclass
@Getter
@Setter
public class BatchTypeJpa extends AbstractEntity {

	private static final int CODE_MAX_LENGTH = 50;
	private static final int NAME_MAX_LENGTH = 100;
	private static final int DESCRIPTION_MAX_LENGTH = 500;
	private static final int CRON_MAX_LENGTH = 100;
	private static final int ZONE_MAX_LENGTH = 50;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = CODE_MAX_LENGTH)
	private BatchTypeCode code;

	@Column(nullable = false, length = NAME_MAX_LENGTH)
	private String name;

	@Column(nullable = false, length = DESCRIPTION_MAX_LENGTH)
	private String description;

	@Column(length = CRON_MAX_LENGTH)
	private String cron;

	@Column(nullable = false, length = ZONE_MAX_LENGTH)
	private String zone;

	@Column(nullable = false)
	private boolean enabled;
}
