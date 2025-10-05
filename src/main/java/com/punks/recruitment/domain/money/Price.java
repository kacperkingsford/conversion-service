package com.punks.recruitment.domain.money;

import com.punks.recruitment.domain.exception.InvalidPriceException;

import java.math.BigDecimal;
import java.util.Objects;

public record Price(BigDecimal value) {
	public Price {
		Objects.requireNonNull(value);
		if (value.signum() <= 0) throw new InvalidPriceException(value);
	}
}
