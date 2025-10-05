package com.punks.recruitment.application.port.out.market;

import com.punks.recruitment.domain.market.MarketSnapshot;
import reactor.core.publisher.Mono;

public interface MarketCachePort {
	Mono<Void> applyPrice(MarketSnapshot snapshot);

	Mono<Void> applyStatus(MarketSnapshot snapshot);
}
