package com.tradingsimulator.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class TradingSimulatorApplication {

	public static void main(String[] args) {
		SpringApplication.run(TradingSimulatorApplication.class, args);
	}

}
