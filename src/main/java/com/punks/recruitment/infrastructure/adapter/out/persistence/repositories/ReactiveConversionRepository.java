package com.punks.recruitment.infrastructure.adapter.out.persistence.repositories;

import com.punks.recruitment.infrastructure.adapter.out.persistence.entities.ConversionEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface ReactiveConversionRepository extends ReactiveCrudRepository<ConversionEntity, UUID> {
	Mono<ConversionEntity> findByCommandId(UUID commandId);

	Mono<ConversionEntity> findByIdempotencyKey(String key);
}
