package com.tradingsimulator.backend.config;

import java.util.stream.Stream;

import org.jspecify.annotations.NonNull;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.tradingsimulator.backend.web.ApiPaths;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

	private static final String[] ALLOWED_METHODS = Stream.of(HttpMethod.GET, HttpMethod.POST, HttpMethod.PUT,
			HttpMethod.PATCH, HttpMethod.DELETE, HttpMethod.OPTIONS)
			.map(HttpMethod::name)
			.toArray(String[]::new);

	private final CorsProperties corsProperties;

	@Override
	public void addCorsMappings(@NonNull CorsRegistry registry) {
		registry.addMapping(ApiPaths.EVERYTHING_UNDER_API)
				.allowedOrigins(corsProperties.allowedOrigins().toArray(String[]::new))
				.allowedMethods(ALLOWED_METHODS)
				.allowedHeaders(CorsConfiguration.ALL)
				.allowCredentials(true);
	}
}
