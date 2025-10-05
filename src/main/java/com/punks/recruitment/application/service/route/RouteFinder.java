package com.punks.recruitment.application.service.route;

import com.punks.recruitment.application.port.out.market.MarketRoutingPort;
import com.punks.recruitment.application.service.convert.RouteQueryFactory;
import com.punks.recruitment.domain.route.*;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class RouteFinder {

	private final MarketRoutingPort router;
	private final RouteQueryFactory routeQueryFactory;

	public RouteFinder(MarketRoutingPort router, RouteQueryFactory routeQueryFactory) {
		this.router = router;
		this.routeQueryFactory = routeQueryFactory;
	}

	public Mono<Route> findRoute(Token from, Token to) {
		var routeQuery = routeQueryFactory.defaultQuery();

		return router.directRate(from, to, routeQuery.maxPriceAge())
				.map(rate -> new Route(List.of(new Hop(from, to, rate.rate(), rate.quotedAt()))))
				.switchIfEmpty(Mono.defer(() ->
						router.findAnyRoute(from, to, routeQuery.maxHops(), routeQuery.maxPriceAge())
				));
	}
}
