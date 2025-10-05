package com.punks.recruitment.domain.market;

import com.punks.recruitment.domain.money.Price;

import java.time.Instant;
import java.util.Objects;

public record MarketPriceChanged(
		MarketId marketId,
		Price price,
		Instant updatedAt
) {
	public MarketPriceChanged {
		Objects.requireNonNull(marketId, "MarketId cannot be null");
		Objects.requireNonNull(price, "Price cannot be null");
		Objects.requireNonNull(updatedAt, "UpdatedAt cannot be null");
	}

	private MarketSnapshot toSnapshot(boolean enabled) {
		return new MarketSnapshot(marketId, price, enabled, updatedAt);
	}

	public MarketSnapshot toEnabledSnapshot() {
		return toSnapshot(true);
	}

	public MarketSnapshot toDisabledSnapshot() {
		return toSnapshot(false);
	}
}

