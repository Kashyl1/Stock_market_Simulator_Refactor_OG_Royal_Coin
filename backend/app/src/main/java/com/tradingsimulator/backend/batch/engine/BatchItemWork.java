package com.tradingsimulator.backend.batch.engine;

@FunctionalInterface
public interface BatchItemWork {

	Object process();
}
