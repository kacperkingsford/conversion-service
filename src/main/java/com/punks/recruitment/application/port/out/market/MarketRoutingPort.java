package com.punks.recruitment.application.port.out.market;

import com.punks.recruitment.domain.money.RateQuote;
import com.punks.recruitment.domain.route.Route;
import com.punks.recruitment.domain.route.Token;
import reactor.core.publisher.Mono;

import java.time.Duration;

public interface MarketRoutingPort {
	Mono<RateQuote> directRate(Token from, Token to, Duration maxPriceAge);

	Mono<Route> findAnyRoute(Token from, Token to, int maxHops, Duration maxPriceAge);
}
