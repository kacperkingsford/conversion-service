package com.punks.recruitment.infrastructure.common.serializer.jackson.exception;

public class DomainEventSerializationException extends RuntimeException {

	public DomainEventSerializationException(String message, Throwable cause) {
		super(message, cause);
	}
}
