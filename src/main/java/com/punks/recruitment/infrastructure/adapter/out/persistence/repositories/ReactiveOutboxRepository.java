package com.punks.recruitment.infrastructure.adapter.out.persistence.repositories;

import com.punks.recruitment.application.port.out.outbox.OutboxRecord;
import com.punks.recruitment.infrastructure.adapter.out.persistence.entities.OutboxEventEntity;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

public interface ReactiveOutboxRepository extends ReactiveCrudRepository<OutboxEventEntity, UUID> {

	@Query("""
			  SELECT event_id, payload_json
			    FROM outbox
			   WHERE published = FALSE
			ORDER BY created_at
			   LIMIT :limit
			  """)
	Flux<OutboxRecord> findUnpublished(int limit);

	@Modifying
	@Query("""
			UPDATE outbox
			   SET published = TRUE,
			       published_at = :publishedAt
			 WHERE event_id = :eventId
			""")
	Mono<Integer> markPublished(UUID eventId, Instant publishedAt);
}
