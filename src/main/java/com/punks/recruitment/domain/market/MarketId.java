package com.punks.recruitment.domain.market;

import com.punks.recruitment.domain.route.Token;

public record MarketId(Token base, Token quote) {
	@Override
	public String toString() {
		return base.value() + "-" + quote.value();
	}
}
