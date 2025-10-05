package com.punks.recruitment.infrastructure.adapter.out.route;

import com.punks.recruitment.application.port.out.clock.ClockPort;
import com.punks.recruitment.application.port.out.market.MarketRoutingPort;
import com.punks.recruitment.application.port.out.market.MarketStateRepositoryPort;
import com.punks.recruitment.application.port.out.precision.PrecisionPolicyPort;
import com.punks.recruitment.domain.market.MarketSnapshot;
import com.punks.recruitment.domain.money.Rate;
import com.punks.recruitment.domain.money.RateQuote;
import com.punks.recruitment.domain.route.Hop;
import com.punks.recruitment.domain.route.Route;
import com.punks.recruitment.domain.route.Token;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Service
public class MarketRouter implements MarketRoutingPort {

	private final MarketStateRepositoryPort marketStateRepositoryPort;
	private final PrecisionPolicyPort precisionPolicy;
	private final ClockPort clock;

	public MarketRouter(MarketStateRepositoryPort marketStateRepositoryPort,
						PrecisionPolicyPort precisionPolicy,
						ClockPort clock) {
		this.marketStateRepositoryPort = marketStateRepositoryPort;
		this.precisionPolicy = precisionPolicy;
		this.clock = clock;
	}

	@Override
	public Mono<RateQuote> directRate(Token from, Token to, Duration maxPriceAge) {
		final Instant cutoff = clock.now().minus(maxPriceAge);
		return marketStateRepositoryPort.findAllEnabledSince(cutoff)
				.collectList()
				.flatMap(markets -> Mono.justOrEmpty(findDirectQuote(markets, from, to)));
	}

	@Override
	public Mono<Route> findAnyRoute(Token from, Token to, int maxHops, Duration maxPriceAge) {
		if (from.equals(to)) {
			return Mono.empty();
		}
		final Instant cutoff = clock.now().minus(maxPriceAge);
		return marketStateRepositoryPort.findAllEnabledSince(cutoff)
				.collectList()
				.flatMap(markets -> Mono.justOrEmpty(bfsAnyRoute(markets, from, to, maxHops)));
	}

	private Optional<RateQuote> findDirectQuote(List<MarketSnapshot> markets, Token from, Token to) {
		for (MarketSnapshot snapshot : markets) {
			if (isPair(snapshot, from, to)) {
				return Optional.of(new RateQuote(
						new Rate(snapshot.price().value()),
						snapshot.updatedAt()
				));
			}
			if (isPair(snapshot, to, from)) {
				final BigDecimal inverse = inverse(snapshot.price().value());
				return Optional.of(new RateQuote(new Rate(inverse), snapshot.updatedAt()));
			}
		}
		return Optional.empty();
	}

	private boolean isPair(MarketSnapshot snapshot, Token base, Token quote) {
		return snapshot.marketId().base().equals(base) && snapshot.marketId().quote().equals(quote);
	}

	private Optional<Route> bfsAnyRoute(List<MarketSnapshot> markets,
										Token from,
										Token to,
										int maxHops) {
		final Map<Token, List<Hop>> adjacency = buildAdjacency(markets);

		final Deque<Node> queue = new ArrayDeque<>();
		final Set<Token> visited = new HashSet<>();

		queue.add(new Node(from, new ArrayList<>()));
		visited.add(from);

		while (!queue.isEmpty()) {
			Node current = queue.poll();

			if (current.path().size() > maxHops) {
				continue;
			}

			if (!current.path().isEmpty() && current.token().equals(to)) {
				return Optional.of(new Route(current.path()));
			}

			for (Hop hop : adjacency.getOrDefault(current.token(), Collections.emptyList())) {
				if (visited.contains(hop.to())) {
					continue;
				}

				List<Hop> extendedPath = new ArrayList<>(current.path());
				extendedPath.add(hop);

				queue.add(new Node(hop.to(), extendedPath));

				if (extendedPath.size() <= maxHops) {
					visited.add(hop.to());
				}
			}
		}

		return Optional.empty();
	}

	private Map<Token, List<Hop>> buildAdjacency(List<MarketSnapshot> markets) {
		final Map<Token, List<Hop>> adjacency = new HashMap<>();

		for (MarketSnapshot snapshot : markets) {
			final Token base = snapshot.marketId().base();
			final Token quote = snapshot.marketId().quote();
			final BigDecimal price = snapshot.price().value();

			addEdge(adjacency, base, quote, price, snapshot.updatedAt());

			if (price.signum() > 0) {
				addEdge(adjacency, quote, base, inverse(price), snapshot.updatedAt());
			}
		}
		return adjacency;
	}

	private void addEdge(Map<Token, List<Hop>> adjacency,
						 Token from,
						 Token to,
						 BigDecimal rateValue,
						 Instant quotedAt) {
		adjacency.computeIfAbsent(from, __ -> new ArrayList<>())
				.add(new Hop(from, to, new Rate(rateValue), quotedAt));
	}

	private BigDecimal inverse(BigDecimal value) {
		return BigDecimal.ONE.divide(value, precisionPolicy.mathContext());
	}

}
