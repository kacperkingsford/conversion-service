package com.punks.recruitment.application.service.market;

import com.punks.recruitment.application.port.in.market.MarketUpdateUseCase;
import com.punks.recruitment.application.port.out.market.MarketCachePort;
import com.punks.recruitment.application.port.out.market.MarketStateRepositoryPort;
import com.punks.recruitment.domain.market.MarketPriceChanged;
import com.punks.recruitment.domain.market.MarketStatusChanged;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

@Service
public class MarketUpdateService implements MarketUpdateUseCase {

	private final MarketStateRepositoryPort markets;
	private final MarketCachePort cache;

	public MarketUpdateService(MarketStateRepositoryPort markets, MarketCachePort cache) {
		this.markets = markets;
		this.cache = cache;
	}

	@Override
	@Transactional
	public Mono<Void> onPriceChanged(MarketPriceChanged event) {
		var snapshot = event.toEnabledSnapshot();
		return markets.upsert(snapshot)
				.then(cache.applyPrice(snapshot))
				.then();
	}

	@Override
	@Transactional
	public Mono<Void> onMarketEnabled(MarketStatusChanged event) {
		return markets.findById(event.marketId())
				.flatMap(existing -> {
					var snapshot = event.toEnabledSnapshot();
					return markets.upsert(snapshot)
							.then(cache.applyStatus(snapshot));
				});
	}

	@Override
	@Transactional
	public Mono<Void> onMarketDisabled(MarketStatusChanged event) {
		return markets.findById(event.marketId())
				.flatMap(existing -> {
					var snapshot = event.toDisabledSnapshot();
					return markets.upsert(snapshot)
							.then(cache.applyStatus(snapshot));
				});
	}
}

