package com.punks.recruitment.infrastructure.adapter.out.cache;

import com.punks.recruitment.application.port.out.market.MarketCachePort;
import com.punks.recruitment.domain.market.MarketSnapshot;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public class InMemoryMarketCache implements MarketCachePort {
	@Override
	public Mono<Void> applyPrice(MarketSnapshot snapshot) {
		// TODO: Noop for now, can be replaced by Caffeine or Redis
		return Mono.empty();
	}

	@Override
	public Mono<Void> applyStatus(MarketSnapshot snapshot) {
		// TODO: Noop for now, can be replaced by Caffeine or Redis
		return Mono.empty();
	}
}
