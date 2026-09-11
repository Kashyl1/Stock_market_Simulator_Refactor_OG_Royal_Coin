package com.tradingsimulator.backend.web;

import java.time.Clock;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.API)
public class HealthController {

	private static final String STATUS_UP = "UP";
	private static final String APPLICATION_NAME = "${spring.application.name}";

	private final String serviceName;
	private final Clock clock;

	public HealthController(@Value(APPLICATION_NAME) String serviceName, Clock clock) {
		this.serviceName = serviceName;
		this.clock = clock;
	}

	@GetMapping(ApiPaths.HEALTH)
	public HealthResponse health() {
		return new HealthResponse(STATUS_UP, serviceName, clock.instant());
	}

	public record HealthResponse(String status, String service, Instant timestamp) {
	}
}
