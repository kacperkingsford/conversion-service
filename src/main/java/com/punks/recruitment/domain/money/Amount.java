package com.punks.recruitment.domain.money;

import com.punks.recruitment.domain.exception.InvalidAmountException;

import java.math.BigDecimal;
import java.util.Objects;

public record Amount(BigDecimal value) {
	public Amount {
		Objects.requireNonNull(value);
		if (value.signum() <= 0) throw new InvalidAmountException(value);
	}
}
