package com.punks.recruitment.infrastructure.adapter.out.persistence;

import com.punks.recruitment.application.port.out.clock.ClockPort;
import com.punks.recruitment.application.port.out.outbox.OutboxPort;
import com.punks.recruitment.domain.route.TokenConvertedEvent;
import com.punks.recruitment.infrastructure.adapter.out.persistence.entities.OutboxEventEntity;
import com.punks.recruitment.infrastructure.adapter.out.persistence.mapper.OutboxEventMapper;
import com.punks.recruitment.infrastructure.adapter.out.persistence.repositories.ReactiveOutboxRepository;
import com.punks.recruitment.infrastructure.common.serializer.DomainEventSerializer;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public class OutboxPortAdapter implements OutboxPort {

	private final ReactiveOutboxRepository repo;
	private final DomainEventSerializer serializer;
	private final ClockPort clockPort;

	public OutboxPortAdapter(ReactiveOutboxRepository repo,
							 DomainEventSerializer serializer, ClockPort clockPort) {
		this.repo = repo;
		this.serializer = serializer;
		this.clockPort = clockPort;
	}

	@Override
	public Mono<Void> append(TokenConvertedEvent event) {
		String payload = serializer.serialize(event);
		OutboxEventEntity entity = OutboxEventMapper.toEntity(event, payload, clockPort.now());
		return repo.save(entity).then();
	}
}
