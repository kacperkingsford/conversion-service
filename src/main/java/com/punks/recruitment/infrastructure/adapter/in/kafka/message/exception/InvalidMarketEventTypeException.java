package com.punks.recruitment.infrastructure.adapter.in.kafka.message.exception;

public class InvalidMarketEventTypeException extends RuntimeException {
	public InvalidMarketEventTypeException(String value) {
		super(String.format("Unknown market event type: %s", value));
	}
}
