package com.punks.recruitment.domain.market;

import com.punks.recruitment.domain.money.Price;

import java.time.Instant;
import java.util.Objects;

public record MarketStatusChanged(
		MarketId marketId,
		Price price,
		Instant updatedAt,
		String reason
) {
	public MarketStatusChanged {
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

