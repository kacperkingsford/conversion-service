package com.punks.recruitment.domain.exception;

import java.math.BigDecimal;

public class InvalidPriceException extends DomainException {
	public InvalidPriceException(BigDecimal value) {
		super(String.format("Price must be > 0, provided: %s", value));
	}
}
