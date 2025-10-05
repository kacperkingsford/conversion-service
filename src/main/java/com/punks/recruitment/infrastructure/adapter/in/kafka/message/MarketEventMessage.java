package com.punks.recruitment.infrastructure.adapter.in.kafka.message;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.Instant;

public record MarketEventMessage(
		@JsonProperty("type") String type,
		// TODO (in proto it would be enum, for ex: MarketPriceChanged | MarketEnabled | MarketDisabled)
		@JsonProperty("marketId") String marketId,
		@JsonProperty("price") BigDecimal price,
		@JsonProperty("updatedAt") Instant updatedAt,
		@JsonProperty("reason") String reason
) implements InboundMessage {
}
