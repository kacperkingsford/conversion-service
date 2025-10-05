package com.punks.recruitment.domain.exception;

import java.math.BigDecimal;

public class InvalidAmountException extends DomainException {
	public InvalidAmountException(BigDecimal value) {
		super(String.format("Amount must be > 0, provided: %s", value));
	}
}
