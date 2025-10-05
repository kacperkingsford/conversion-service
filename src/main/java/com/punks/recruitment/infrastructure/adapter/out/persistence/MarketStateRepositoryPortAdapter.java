package com.punks.recruitment.infrastructure.adapter.out.persistence;

import com.punks.recruitment.application.port.out.market.MarketStateRepositoryPort;
import com.punks.recruitment.domain.market.MarketId;
import com.punks.recruitment.domain.market.MarketSnapshot;
import com.punks.recruitment.infrastructure.adapter.out.persistence.mapper.MarketEntityMapper;
import com.punks.recruitment.infrastructure.adapter.out.persistence.repositories.ReactiveMarketRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Repository
public class MarketStateRepositoryPortAdapter implements MarketStateRepositoryPort {
	private final ReactiveMarketRepository repository;

	public MarketStateRepositoryPortAdapter(ReactiveMarketRepository repository) {
		this.repository = repository;
	}

	@Override
	public Mono<Void> upsert(MarketSnapshot snapshot) {
		return repository.upsert(MarketEntityMapper.toEntity(snapshot))
				.then();
	}

	@Override
	public Mono<MarketSnapshot> findById(MarketId id) {
		return repository.findByMarketId(id.toString())
				.map(MarketEntityMapper::toSnapshot);
	}

	@Override
	public Flux<MarketSnapshot> findAllEnabledSince(Instant cutoff) {
		return repository.findAllByEnabledAndUpdatedAtAfter(Boolean.TRUE, cutoff)
				.map(MarketEntityMapper::toSnapshot);
	}
}
