package com.tradingsimulator.backend.batch;

import java.time.Instant;

public record BatchStopRequest(String requestedBy, Instant requestedAt) {
}
