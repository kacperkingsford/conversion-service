package com.punks.recruitment.domain.exception;

public class InvalidTokenException extends DomainException {
	public InvalidTokenException(String value) {
		super("Invalid token: " + value);
	}
}
