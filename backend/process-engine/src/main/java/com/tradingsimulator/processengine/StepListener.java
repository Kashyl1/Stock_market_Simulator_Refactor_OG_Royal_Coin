package com.tradingsimulator.processengine;

public interface StepListener<C extends ProcessContext> {

	String processKey();

	StepKey step();

	StepResult execute(StepExecution<C> execution);
}
