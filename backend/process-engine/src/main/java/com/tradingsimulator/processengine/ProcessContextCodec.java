package com.tradingsimulator.processengine;

import org.springframework.stereotype.Component;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

@Component
public class ProcessContextCodec {

	private final ObjectMapper mapper;

	public ProcessContextCodec(ObjectMapper mapper) {
		this.mapper = mapper;
	}

	public ProcessContextCodec() {
		this(JsonMapper.builder().build());
	}

	public String encode(ProcessContext context) {
		try {
			return mapper.writeValueAsString(context);
		}
		catch (RuntimeException e) {
			throw new ProcessExecutionException("failed to serialise process context "
					+ context.getClass().getName(), e);
		}
	}

	public <C extends ProcessContext> C decode(String json, Class<C> type) {
		try {
			return mapper.readValue(json, type);
		}
		catch (RuntimeException e) {
			throw new ProcessExecutionException("failed to deserialise process context " + type.getName(), e);
		}
	}

	public JsonNode toNode(String json) {
		try {
			return mapper.readTree(json);
		}
		catch (RuntimeException e) {
			throw new ProcessExecutionException("failed to parse process context JSON", e);
		}
	}
}
