package com.punks.recruitment.infrastructure.adapter.out.persistence;

import com.punks.recruitment.application.port.out.convert.ConversionRepository;
import com.punks.recruitment.domain.convert.ConversionRecord;
import com.punks.recruitment.domain.id.CommandId;
import com.punks.recruitment.domain.id.IdempotencyKey;
import com.punks.recruitment.domain.route.Route;
import com.punks.recruitment.infrastructure.adapter.out.persistence.entities.ConversionEntity;
import com.punks.recruitment.infrastructure.adapter.out.persistence.mapper.ConversionEntityMapper;
import com.punks.recruitment.infrastructure.adapter.out.persistence.repositories.ReactiveConversionRepository;
import com.punks.recruitment.infrastructure.common.deserializer.DomainPayloadDeserializer;
import com.punks.recruitment.infrastructure.common.serializer.DomainEventSerializer;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

@Repository
public class ConversionRepositoryAdapter implements ConversionRepository {

	private final ReactiveConversionRepository repository;
	private final DomainEventSerializer serializer;
	private final DomainPayloadDeserializer deserializer;

	public ConversionRepositoryAdapter(ReactiveConversionRepository repository,
									   DomainEventSerializer serializer,
									   DomainPayloadDeserializer deserializer) {
		this.repository = repository;
		this.serializer = serializer;
		this.deserializer = deserializer;
	}

	@Override
	public Mono<ConversionRecord> saveIfAbsent(ConversionRecord conversionRecord) {
		String routeJson = serializeRouteSafe(conversionRecord.route());
		ConversionEntity entity = ConversionEntityMapper.toEntity(conversionRecord, routeJson);

		return repository.save(entity)
				.flatMap(saved -> Mono.just(ConversionEntityMapper.toDomain(saved, deserializeRouteSafe(saved.getPathJson().asString()))))
				.onErrorResume(DataIntegrityViolationException.class, ex -> {
					Mono<ConversionEntity> byCmd = repository.findByCommandId(conversionRecord.commandId().value());
					Mono<ConversionEntity> byKey = conversionRecord.idempotencyKey()
							.map(IdempotencyKey::value)
							.map(repository::findByIdempotencyKey)
							.orElse(Mono.empty());

					return byCmd.switchIfEmpty(byKey)
							.map(found -> ConversionEntityMapper.toDomain(found, deserializeRouteSafe(found.getPathJson().asString())));
				});
	}

	@Override
	public Mono<Optional<ConversionRecord>> findByCommand(CommandId commandId) {
		return repository.findByCommandId(commandId.value())
				.map(e -> Optional.of(ConversionEntityMapper.toDomain(e, deserializeRouteSafe(e.getPathJson().asString()))))
				.defaultIfEmpty(Optional.empty());
	}

	@Override
	public Mono<Optional<ConversionRecord>> findByIdempotency(IdempotencyKey key) {
		return repository.findByIdempotencyKey(key.value())
				.map(e -> Optional.of(ConversionEntityMapper.toDomain(e, deserializeRouteSafe(e.getPathJson().asString()))))
				.defaultIfEmpty(Optional.empty());
	}

	private String serializeRouteSafe(Route route) {
		Route nonNull = (route != null) ? route : new Route(List.of());
		return serializer.serialize(nonNull);
	}

	private Route deserializeRouteSafe(String json) {
		if (json == null || json.isBlank()) {
			return new Route(List.of());
		}
		try {
			return deserializer.deserialize(json, Route.class);
		} catch (RuntimeException ex) {
			return new Route(List.of());
		}
	}
}
