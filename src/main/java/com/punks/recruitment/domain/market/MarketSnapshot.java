package com.punks.recruitment.domain.market;

import com.punks.recruitment.domain.money.Price;

import java.time.Instant;

public record MarketSnapshot(MarketId marketId, Price price, boolean enabled, Instant updatedAt) {
}
