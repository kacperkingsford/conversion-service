package com.punks.recruitment.domain.route;

import com.punks.recruitment.domain.exception.InvalidTokenException;

import java.util.Objects;

public record Token(String value) {
	public Token {
		Objects.requireNonNull(value);
		if (!value.matches("[A-Z0-9]{1,12}")) throw new InvalidTokenException(value);
	}
}
