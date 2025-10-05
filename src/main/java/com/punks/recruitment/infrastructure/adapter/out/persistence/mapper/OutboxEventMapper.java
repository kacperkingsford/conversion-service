package com.punks.recruitment.infrastructure.adapter.out.persistence.mapper;

import com.punks.recruitment.domain.route.TokenConvertedEvent;
import com.punks.recruitment.infrastructure.adapter.out.persistence.entities.OutboxEventEntity;
import io.r2dbc.postgresql.codec.Json;

import java.time.Instant;

public class OutboxEventMapper {

	private OutboxEventMapper() {
	}

	public static OutboxEventEntity toEntity(TokenConvertedEvent event, String payloadJson, Instant createdAt) {
		OutboxEventEntity entity = new OutboxEventEntity();
		entity.setAggregateType("Conversion");
		entity.setAggregateId(event.conversionId().value().toString());
		entity.setEventType("TokenConvertedEvent");
		entity.setPayloadJson(Json.of(payloadJson));
		entity.setCreatedAt(createdAt);
		entity.setPublished(Boolean.FALSE);
		entity.setPublishedAt(null);
		return entity;
	}
}
