package com.punks.recruitment.domain.exception;

import java.math.BigDecimal;

public class InvalidRateException extends DomainException {
	public InvalidRateException(BigDecimal value) {
		super(String.format("Rate must be > 0, provided: %s", value));
	}
}
