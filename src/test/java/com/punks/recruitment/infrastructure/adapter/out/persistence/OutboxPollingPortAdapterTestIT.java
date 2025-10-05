package com.punks.recruitment.infrastructure.adapter.out.persistence;

import com.punks.recruitment.config.AbstractR2dbcPostgresIT;
import com.punks.recruitment.infrastructure.adapter.out.persistence.entities.OutboxEventEntity;
import com.punks.recruitment.infrastructure.adapter.out.persistence.repositories.ReactiveOutboxRepository;
import io.r2dbc.postgresql.codec.Json;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Import(OutboxPollingPortAdapter.class)
class OutboxPollingPortAdapterTestIT extends AbstractR2dbcPostgresIT {

	@Autowired
	ReactiveOutboxRepository reactiveOutboxRepository;

	@Autowired
	OutboxPollingPortAdapter adapter;

	@BeforeEach
	void clean() {
		StepVerifier.create(reactiveOutboxRepository.deleteAll()).verifyComplete();
	}

	@Test
	void fetchUnpublishedReturnsUnpublishedOrderedByCreatedAt() {
		Instant t1 = Instant.parse("2024-01-01T00:00:00Z");
		Instant t2 = t1.plusSeconds(10);
		Instant t3 = t1.plusSeconds(20);

		OutboxEventEntity e1 = newOutbox(false, t1, "{\"a\":1}");
		OutboxEventEntity e2 = newOutbox(false, t2, "{\"b\":2}");
		OutboxEventEntity e3 = newOutbox(true, t3, "{\"c\":3}");

		Mono<Void> setup = reactiveOutboxRepository.saveAll(List.of(e1, e2, e3)).then();

		StepVerifier.create(setup.thenMany(adapter.fetchUnpublished(10)).collectList())
				.assertNext(list -> {
					assertEquals(2, list.size());
					assertEquals(e1.getEventId(), list.get(0).eventId());
					assertEquals(e2.getEventId(), list.get(1).eventId());
				})
				.verifyComplete();
	}

	@Test
	void markPublishedSetsFlagAndTimestamp() {
		Instant createdAt = Instant.parse("2024-01-01T00:00:00Z");
		Instant publishedAt = Instant.parse("2024-01-01T00:01:00Z");

		OutboxEventEntity e = newOutbox(false, createdAt, "{\"x\":42}");

		StepVerifier.create(
						reactiveOutboxRepository.save(e)
								.flatMap(saved -> adapter.markPublished(saved.getEventId(), publishedAt)
										.then(reactiveOutboxRepository.findById(saved.getEventId()))
								)
				)
				.assertNext(found -> {
					assertTrue(found.getPublished());
					assertEquals(publishedAt, found.getPublishedAt());
				})
				.verifyComplete();
	}

	private OutboxEventEntity newOutbox(boolean published, Instant createdAt, String payloadJson) {
		OutboxEventEntity e = new OutboxEventEntity();
		e.setAggregateType("conversion");
		e.setAggregateId("agg-1");
		e.setEventType("CREATED");
		e.setPayloadJson(Json.of(payloadJson));
		e.setCreatedAt(createdAt);
		e.setPublished(published);
		e.setPublishedAt(null);
		return e;
	}
}
