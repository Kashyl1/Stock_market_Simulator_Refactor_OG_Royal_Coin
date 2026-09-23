package com.tradingsimulator.backend.support;

import com.tradingsimulator.backend.auth.service.ClientDetails;

public final class TestClients {

	public static final String USER_AGENT = "JUnit";
	public static final String IP = "127.0.0.1";

	public static ClientDetails details() {
		return new ClientDetails(USER_AGENT, IP);
	}

	private TestClients() { }
}
