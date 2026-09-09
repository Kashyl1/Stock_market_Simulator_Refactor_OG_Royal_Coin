package com.tradingsimulator.processengine;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class ProcessRegistry implements SmartInitializingSingleton {

	private final Map<String, ProcessDefinition<?>> definitions = new LinkedHashMap<>();
	private final Map<String, Map<String, StepListener<?>>> listeners = new LinkedHashMap<>();

	public ProcessRegistry(List<ProcessDefinition<?>> definitionBeans, List<StepListener<?>> listenerBeans) {
		for (ProcessDefinition<?> def : definitionBeans) {
			if (definitions.put(def.key(), def) != null) {
				throw new ProcessDefinitionException("duplicate process definition for key '" + def.key() + "'");
			}
		}
		for (StepListener<?> listener : listenerBeans) {
			Map<String, StepListener<?>> byStep = listeners.computeIfAbsent(listener.processKey(),
					k -> new LinkedHashMap<>());
			if (byStep.put(listener.step().name(), listener) != null) {
				throw new ProcessDefinitionException("duplicate step listener for '" + listener.processKey()
						+ "/" + listener.step().name() + "'");
			}
		}
	}

	@Override
	public void afterSingletonsInstantiated() {
		validate();
	}

	public void validate() {
		listeners.forEach((processKey, byStep) -> {
			ProcessDefinition<?> def = definitions.get(processKey);
			if (def == null) {
				throw new ProcessDefinitionException("step listener(s) registered for unknown process '"
						+ processKey + "'");
			}
			Set<String> stepNames = def.steps().stream().map(StepKey::name).collect(Collectors.toSet());
			for (String handledStep : byStep.keySet()) {
				if (!stepNames.contains(handledStep)) {
					throw new ProcessDefinitionException("process '" + processKey + "': listener for step '"
							+ handledStep + "' which is not in the definition");
				}
			}
		});

		for (ProcessDefinition<?> def : definitions.values()) {
			Map<String, StepListener<?>> byStep = listeners.getOrDefault(def.key(), Map.of());
			for (StepKey step : def.steps()) {
				if (!def.isEndStep(step) && !byStep.containsKey(step.name())) {
					throw new ProcessDefinitionException("process '" + def.key() + "': no listener for step '"
							+ step.name() + "'");
				}
			}
		}

		log.info("Process engine ready: {} definition(s) {}", definitions.size(), definitions.keySet());
	}

	public ProcessDefinition<?> definition(String key) {
		ProcessDefinition<?> def = definitions.get(key);
		if (def == null) {
			throw new UnknownProcessException(key);
		}
		return def;
	}

	public Optional<StepListener<?>> listener(String processKey, StepKey step) {
		return Optional.ofNullable(listeners.getOrDefault(processKey, Map.of()).get(step.name()));
	}

	public Set<String> definitionKeys() {
		return Set.copyOf(definitions.keySet());
	}
}
