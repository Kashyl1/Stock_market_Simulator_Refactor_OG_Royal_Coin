package com.tradingsimulator.backend.batch.web.dto;

import java.time.Instant;

import com.tradingsimulator.backend.batch.BatchTypeCode;

import jakarta.validation.constraints.NotNull;

public record PlanBatchJobRequest(@NotNull BatchTypeCode type, Instant scheduledFor) {
}
