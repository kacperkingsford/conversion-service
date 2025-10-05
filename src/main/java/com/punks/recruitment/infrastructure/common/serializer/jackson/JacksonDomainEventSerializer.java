package com.punks.recruitment.infrastructure.common.serializer.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.punks.recruitment.infrastructure.common.serializer.DomainEventSerializer;
import com.punks.recruitment.infrastructure.common.serializer.jackson.exception.DomainEventSerializationException;

public class JacksonDomainEventSerializer implements DomainEventSerializer {
	private final ObjectMapper objectMapper;

	public JacksonDomainEventSerializer(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Override
	public String serialize(Object event) {
		try {
			return objectMapper.writeValueAsString(event);
		} catch (Exception e) {
			throw new DomainEventSerializationException("Failed to serialize domain event", e);
		}
	}
}
