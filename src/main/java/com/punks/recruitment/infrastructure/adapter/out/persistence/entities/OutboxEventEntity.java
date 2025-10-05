package com.punks.recruitment.infrastructure.adapter.out.persistence.entities;

import io.r2dbc.postgresql.codec.Json;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Table("outbox")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
}
