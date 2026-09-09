package com.tradingsimulator.processengine;

import java.util.UUID;

public class ProcessClosedException extends RuntimeException {

	public ProcessClosedException(UUID id, ProcessStatus status) {
		super("process instance " + id + " is " + status + " and can no longer be rewound");
	}
}
