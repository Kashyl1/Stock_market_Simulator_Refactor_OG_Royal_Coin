package com.tradingsimulator.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.tradingsimulator")
@EntityScan(basePackages = "com.tradingsimulator")
@EnableJpaRepositories(basePackages = "com.tradingsimulator")
public class TradingSimulatorApplication {

	public static void main(String[] args) {
		SpringApplication.run(TradingSimulatorApplication.class, args);
	}

}
