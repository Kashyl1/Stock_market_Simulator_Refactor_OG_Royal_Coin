package com.tradingsimulator.processengine.store;

import java.time.Instant;
import java.util.UUID;

import com.tradingsimulator.processengine.ProcessStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(fluent = true)
@AllArgsConstructor
public final class ProcessRecord {

	private final UUID id;
	private final String definitionKey;
	private String currentStep;
	private ProcessStatus status;
	private String contextJson;
	private final Instant createdAt;
	private Instant updatedAt;
}
