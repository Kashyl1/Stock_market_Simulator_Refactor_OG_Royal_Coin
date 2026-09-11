package com.tradingsimulator.backend.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import com.tradingsimulator.processengine.ProcessEngine;

@Configuration(proxyBeanMethods = false)
@ComponentScan(basePackageClasses = ProcessEngine.class)
public class ProcessEngineConfig {
}
