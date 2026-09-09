package com.tradingsimulator.processengine;

public class UnknownProcessException extends RuntimeException {

	public UnknownProcessException(String key) {
		super("no process definition registered for key '" + key + "'");
	}
}
