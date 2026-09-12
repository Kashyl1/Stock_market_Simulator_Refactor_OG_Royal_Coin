package com.tradingsimulator.backend.config;

import java.security.SecureRandom;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class RandomConfig {

	@Bean
	SecureRandom secureRandom() {
		return new SecureRandom();
	}
}
