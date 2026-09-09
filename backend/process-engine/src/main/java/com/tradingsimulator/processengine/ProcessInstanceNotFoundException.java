package com.tradingsimulator.processengine;

import java.util.UUID;

public class ProcessInstanceNotFoundException extends RuntimeException {

	public ProcessInstanceNotFoundException(UUID id) {
		super("no process instance found for id " + id);
	}
}
