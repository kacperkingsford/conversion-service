package com.punks.recruitment.domain.money;

import com.punks.recruitment.domain.exception.InvalidRateException;

import java.math.BigDecimal;
import java.util.Objects;

public record Rate(BigDecimal value) {
	public Rate {
		Objects.requireNonNull(value);
		if (value.signum() <= 0) throw new InvalidRateException(value);
	}
}
