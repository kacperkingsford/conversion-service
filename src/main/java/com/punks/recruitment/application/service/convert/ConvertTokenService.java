package com.punks.recruitment.application.service.convert;

import com.punks.recruitment.application.port.in.convert.ConvertTokenUseCase;
import com.punks.recruitment.application.port.out.clock.ClockPort;
import com.punks.recruitment.application.port.out.convert.ConversionRepository;
import com.punks.recruitment.application.port.out.outbox.OutboxPort;
import com.punks.recruitment.application.port.out.precision.PrecisionPolicyPort;
import com.punks.recruitment.application.service.convert.dto.ConversionResult;
import com.punks.recruitment.application.service.convert.mapper.ConversionMapper;
import com.punks.recruitment.application.service.route.RouteFinder;
import com.punks.recruitment.domain.convert.*;
import com.punks.recruitment.domain.exception.NoRouteException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.util.Optional;

@Service
public class ConvertTokenService implements ConvertTokenUseCase {

	private final ConversionRepository conversions;
	private final OutboxPort outbox;
	private final ClockPort clock;
	private final PrecisionPolicyPort precision;
	private final RouteFinder routeFinder;

	public ConvertTokenService(ConversionRepository conversions, OutboxPort outbox,
							   ClockPort clock, PrecisionPolicyPort precision,
							   RouteFinder routeFinder) {
		this.routeFinder = routeFinder;
		this.conversions = conversions;
		this.outbox = outbox;
		this.clock = clock;
		this.precision = precision;
	}

	@Override
	@Transactional
	public Mono<ConversionResult> convert(ConvertCommand convertCommand) {
		return findExistingConversion(convertCommand)
				.switchIfEmpty(Mono.defer(() -> computeAndPersist(convertCommand)))
				.map(ConversionMapper::toResult);
	}

	private Mono<ConversionRecord> findExistingConversion(ConvertCommand convertCommand) {
		Mono<Optional<ConversionRecord>> byCmd = conversions.findByCommand(convertCommand.commandId());
		Mono<Optional<ConversionRecord>> byKey = convertCommand.idempotencyKey()
				.map(conversions::findByIdempotency)
				.orElse(Mono.just(Optional.empty()));

		return Mono.zip(byCmd, byKey)
				.map(tuple -> tuple.getT1().or(tuple::getT2))
				.flatMap(opt -> opt.map(Mono::just).orElse(Mono.empty()));
	}

	private Mono<ConversionRecord> computeAndPersist(ConvertCommand convertCommand) {
		return routeFinder.findRoute(convertCommand.tokenFrom(), convertCommand.tokenTo())
				.switchIfEmpty(Mono.error(new NoRouteException(convertCommand.tokenFrom(), convertCommand.tokenTo())))
				.map(route -> ConversionRecord.succeeded(convertCommand, route.convert(convertCommand.amount(), precision), route, clock.now()))
				.flatMap(this::saveAndPublish)
				.onErrorResume(NoRouteException.class, ex -> saveFailedNoRoute(convertCommand));
	}

	private Mono<ConversionRecord> saveAndPublish(ConversionRecord conversionRecord) {
		return conversions.saveIfAbsent(conversionRecord)
				.flatMap(saved -> isNew(saved, conversionRecord)
						? outbox.append(saved.toEvent()).thenReturn(saved)
						: Mono.just(saved));
	}

	private Mono<ConversionRecord> saveFailedNoRoute(ConvertCommand cmd) {
		return conversions.saveIfAbsent(ConversionRecord.failedNoRoute(cmd, clock.now()));
	}

	private boolean isNew(ConversionRecord saved, ConversionRecord attempted) {
		return saved.conversionId().equals(attempted.conversionId());
	}
}



