package com.punks.recruitment.infrastructure.adapter.out.persistence;

import com.punks.recruitment.application.port.out.outbox.OutboxPollingPort;
import com.punks.recruitment.application.port.out.outbox.OutboxRecord;
import com.punks.recruitment.infrastructure.adapter.out.persistence.repositories.ReactiveOutboxRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

@Repository
public class OutboxPollingPortAdapter implements OutboxPollingPort {
	private final ReactiveOutboxRepository reactiveOutboxRepository;

	public OutboxPollingPortAdapter(ReactiveOutboxRepository reactiveOutboxRepository) {
		this.reactiveOutboxRepository = reactiveOutboxRepository;
	}

	@Override
	public Flux<OutboxRecord> fetchUnpublished(int batchSize) {
		return reactiveOutboxRepository.findUnpublished(batchSize);
	}


	@Override
	public Mono<Void> markPublished(UUID eventId, Instant publishedAt) {
		return reactiveOutboxRepository.markPublished(eventId, publishedAt)
				.then();
	}
}
