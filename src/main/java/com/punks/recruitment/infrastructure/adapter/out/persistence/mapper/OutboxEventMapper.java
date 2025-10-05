package com.punks.recruitment.infrastructure.adapter.out.persistence.mapper;

import com.punks.recruitment.domain.route.TokenConvertedEvent;
import com.punks.recruitment.infrastructure.adapter.out.persistence.entities.OutboxEventEntity;
import io.r2dbc.postgresql.codec.Json;

import java.time.Instant;

public class OutboxEventMapper {

	private OutboxEventMapper() {
	}

	public static OutboxEventEntity toEntity(TokenConvertedEvent event, String payloadJson, Instant createdAt) {
		return OutboxEventEntity.builder()
				.aggregateType("Conversion")
				.aggregateId(event.conversionId().value().toString())
				.eventType("TokenConvertedEvent")
				.payloadJson(Json.of(payloadJson))
				.createdAt(createdAt)
				.published(false)
				.publishedAt(null)
				.build();
	}
}
