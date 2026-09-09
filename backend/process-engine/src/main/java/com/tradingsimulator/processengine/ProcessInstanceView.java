package com.tradingsimulator.processengine;

import java.time.Instant;
import java.util.UUID;

import tools.jackson.databind.JsonNode;

public record ProcessInstanceView(
		UUID id,
		String definitionKey,
		String currentStep,
		ProcessStatus status,
		JsonNode context,
		Instant createdAt,
		Instant updatedAt) {
}
