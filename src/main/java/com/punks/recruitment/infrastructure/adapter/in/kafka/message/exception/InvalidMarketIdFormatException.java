package com.punks.recruitment.infrastructure.adapter.in.kafka.message.exception;

public class InvalidMarketIdFormatException extends RuntimeException {
    public InvalidMarketIdFormatException(String value) {
        super(String.format("Invalid marketId format! Supported: XYZ-ABC, provided: %s", value));
    }
}
