package com.punks.recruitment.domain.money;

import com.punks.recruitment.domain.route.Token;

import java.math.MathContext;

public interface PrecisionPolicy {
	MathContext mathContext();

	int scaleFor(Token token);
}
