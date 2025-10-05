package com.punks.recruitment.application.port.out.outbox;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

public interface OutboxPollingPort {
	Flux<OutboxRecord> fetchUnpublished(int batchSize);

	Mono<Void> markPublished(UUID eventId, Instant publishedAt);
}
