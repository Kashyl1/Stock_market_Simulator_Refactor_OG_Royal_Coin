package com.tradingsimulator.processengine;

import java.util.UUID;

public class IllegalProcessStateException extends RuntimeException {

	public IllegalProcessStateException(UUID id, ProcessStatus actual) {
		super("process instance " + id + " is " + actual + "; expected WAITING");
	}
}
