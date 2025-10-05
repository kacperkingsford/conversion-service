package com.punks.recruitment.application.port.out.convert;

import com.punks.recruitment.domain.id.CommandId;
import com.punks.recruitment.domain.convert.ConversionRecord;
import com.punks.recruitment.domain.id.IdempotencyKey;
import reactor.core.publisher.Mono;

import java.util.Optional;

public interface ConversionRepository {
	Mono<ConversionRecord> saveIfAbsent(ConversionRecord conversionRecord);

	Mono<Optional<ConversionRecord>> findByCommand(CommandId commandId);

	Mono<Optional<ConversionRecord>> findByIdempotency(IdempotencyKey key);
}
