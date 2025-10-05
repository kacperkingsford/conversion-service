package com.punks.recruitment.infrastructure.common.deserializer;

public interface DomainPayloadDeserializer {
    <T> T deserialize(String payload, Class<T> type);
}
