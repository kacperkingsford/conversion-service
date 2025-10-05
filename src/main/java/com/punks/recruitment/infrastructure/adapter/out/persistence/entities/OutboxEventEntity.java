package com.punks.recruitment.infrastructure.adapter.out.persistence.entities;

import io.r2dbc.postgresql.codec.Json;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Table("outbox")
public class OutboxEventEntity {
	@Id
	@Column("event_id")
	private UUID eventId;

	@Column("aggregate_type")
	private String aggregateType;

	@Column("aggregate_id")
	private String aggregateId;

	@Column("event_type")
	private String eventType; //TODO: "TokenConvertedEvent", może być enum + @Enumerated(String)

	@Column("payload_json")
	private Json payloadJson;

	@Column("created_at")
	private Instant createdAt;

	@Column("published")
	private Boolean published;

	@Column("published_at")
	private Instant publishedAt;

	public UUID getEventId() {
		return eventId;
	}

	public void setEventId(UUID eventId) {
		this.eventId = eventId;
	}

	public String getAggregateType() {
		return aggregateType;
	}

	public void setAggregateType(String aggregateType) {
		this.aggregateType = aggregateType;
	}

	public String getAggregateId() {
		return aggregateId;
	}

	public void setAggregateId(String aggregateId) {
		this.aggregateId = aggregateId;
	}

	public String getEventType() {
		return eventType;
	}

	public void setEventType(String eventType) {
		this.eventType = eventType;
	}

	public Json getPayloadJson() {
		return payloadJson;
	}

	public void setPayloadJson(Json payloadJson) {
		this.payloadJson = payloadJson;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}

	public Boolean getPublished() {
		return published;
	}

	public void setPublished(Boolean published) {
		this.published = published;
	}

	public Instant getPublishedAt() {
		return publishedAt;
	}

	public void setPublishedAt(Instant publishedAt) {
		this.publishedAt = publishedAt;
	}
}
