package com.punks.recruitment.domain.route;

import com.punks.recruitment.domain.money.Amount;
import com.punks.recruitment.domain.money.PrecisionPolicy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public record Route(List<Hop> hops) {
	public Amount convert(Amount amountIn, PrecisionPolicy precisionPolicy) {
		var mc = precisionPolicy.mathContext();
		BigDecimal acc = amountIn.value();

		for (Hop hop : hops) {
			acc = acc.multiply(hop.rate().value(), mc);
		}

		int scale = precisionPolicy.scaleFor(hops.getLast().to());
		acc = acc.setScale(scale, RoundingMode.HALF_UP);

		return new Amount(acc);
	}

	public boolean isEmpty() {
		return hops == null || hops.isEmpty();
	}
}
