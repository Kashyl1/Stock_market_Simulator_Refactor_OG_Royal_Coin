package com.tradingsimulator.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.tradingsimulator.backend.support.TestJwtKeys;
import com.tradingsimulator.backend.support.TestProfiles;

@SpringBootTest
@ActiveProfiles(TestProfiles.TEST)
class TradingSimulatorApplicationTests {

	@DynamicPropertySource
	static void jwtKeys(DynamicPropertyRegistry registry) {
		TestJwtKeys.register(registry);
	}

	@Test
	void contextLoads() {
	}

}
