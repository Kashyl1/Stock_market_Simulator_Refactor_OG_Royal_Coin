package com.tradingsimulator.backend.common.persistence;

import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import com.tradingsimulator.backend.TradingSimulatorApplication;
import com.tradingsimulator.processengine.store.jpa.ProcessInstanceEntity;

@Configuration(proxyBeanMethods = false)
@EntityScan(basePackageClasses = { TradingSimulatorApplication.class, ProcessInstanceEntity.class })
@EnableJpaRepositories(basePackageClasses = { TradingSimulatorApplication.class, ProcessInstanceEntity.class })
public class PersistenceConfig {
}
