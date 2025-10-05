package com.punks.recruitment.infrastructure.common.deserializer.jackson.exception;

public class DomainEventDeserializationException extends RuntimeException {

    public DomainEventDeserializationException(String message, Throwable cause) {
        super(message, cause);
    }
}
