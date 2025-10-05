package com.punks.recruitment.infrastructure.common.deserializer.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.punks.recruitment.infrastructure.common.deserializer.DomainPayloadDeserializer;
import com.punks.recruitment.infrastructure.common.deserializer.jackson.exception.DomainEventDeserializationException;

public class JacksonDomainPayloadDeserializer implements DomainPayloadDeserializer {

    private final ObjectMapper objectMapper;

    public JacksonDomainPayloadDeserializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public <T> T deserialize(String payload, Class<T> type) {
        try {
            return objectMapper.readValue(payload, type);
        } catch (Exception e) {
            throw new DomainEventDeserializationException(String.format("Failed to deserialize payload to %s", type.getSimpleName()), e);
        }
    }
}
