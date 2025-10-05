package com.punks.recruitment.application.port.out.market;

import com.punks.recruitment.domain.market.MarketId;
import com.punks.recruitment.domain.market.MarketSnapshot;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

public interface MarketStateRepositoryPort {
	Mono<Void> upsert(MarketSnapshot snapshot);

	Mono<MarketSnapshot> findById(MarketId id);

	Flux<MarketSnapshot> findAllEnabledSince(Instant cutoff);
}
