package com.punks.recruitment.infrastructure.adapter.in.kafka.message;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ConvertTokenMessage(
		@JsonProperty("commandId") UUID commandId,
		@JsonProperty("idempotencyKey") String idempotencyKey,
		@JsonProperty("tokenFrom") String tokenFrom,
		@JsonProperty("tokenTo") String tokenTo,
		@JsonProperty("amount") BigDecimal amount,
		@JsonProperty("requestedAt") Instant requestedAt
) implements InboundMessage {
}
