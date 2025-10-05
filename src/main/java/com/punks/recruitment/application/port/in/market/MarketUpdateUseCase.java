package com.punks.recruitment.application.port.in.market;

import com.punks.recruitment.domain.market.MarketPriceChanged;
import com.punks.recruitment.domain.market.MarketStatusChanged;
import reactor.core.publisher.Mono;

public interface MarketUpdateUseCase {
	Mono<Void> onPriceChanged(MarketPriceChanged event);

	Mono<Void> onMarketEnabled(MarketStatusChanged event);

	Mono<Void> onMarketDisabled(MarketStatusChanged event);
}
