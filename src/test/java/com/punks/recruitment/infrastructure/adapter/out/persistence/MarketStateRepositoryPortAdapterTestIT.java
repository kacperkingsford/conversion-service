package com.punks.recruitment.infrastructure.adapter.out.persistence;

import com.punks.recruitment.config.AbstractR2dbcPostgresIT;
import com.punks.recruitment.domain.market.MarketId;
import com.punks.recruitment.domain.market.MarketSnapshot;
import com.punks.recruitment.domain.money.Price;
import com.punks.recruitment.domain.route.Token;
import com.punks.recruitment.infrastructure.adapter.out.persistence.repositories.ReactiveMarketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

@Import(MarketStateRepositoryPortAdapter.class)
class MarketStateRepositoryPortAdapterTestIT extends AbstractR2dbcPostgresIT {

	@Autowired
	MarketStateRepositoryPortAdapter adapter;
	@Autowired
	ReactiveMarketRepository repository;

	@BeforeEach
	void clean() {
		StepVerifier.create(repository.deleteAll()).verifyComplete();
	}

	@Test
	void upsertInsertsWhenAbsentAndFindByIdReturnsSnapshot() {
		// given
		MarketId id = id("BTC", "USDT");
		Instant t1 = Instant.parse("2025-01-01T10:00:00Z");
		MarketSnapshot s = snap(id, "30000.000000000000000000", true, t1);

		// when
		Mono<MarketSnapshot> probe = adapter.upsert(s).then(adapter.findById(id));

		// then
		StepVerifier.create(probe)
				.assertNext(got -> {
					assertEquals(id.toString(), got.marketId().toString());
					assertEquals(new BigDecimal("30000.000000000000000000"), got.price().value());
					assertTrue(got.enabled());
					assertEquals(t1, got.updatedAt());
				})
				.verifyComplete();

		StepVerifier.create(repository.findByMarketId(id.toString()))
				.assertNext(market -> {
					assertEquals("BTC", market.getBaseToken());
					assertEquals("USDT", market.getQuoteToken());
				})
				.verifyComplete();

	}

	@Test
	void upsertUpdatesWhenPresentOverwritesPriceEnabledAndTimestamp() {
		// given
		MarketId id = id("ETH", "USDT");
		Instant t1 = Instant.parse("2025-01-01T10:00:00Z");
		MarketSnapshot v1 = snap(id, "2000", true, t1);

		Instant t2 = t1.plusSeconds(60);
		MarketSnapshot v2 = snap(id, "2100.550000000000000000", false, t2);

		// when
		Mono<MarketSnapshot> probe = adapter.upsert(v1)
				.then(adapter.upsert(v2))
				.then(adapter.findById(id));

		// then
		StepVerifier.create(probe)
				.assertNext(got -> {
					assertEquals(new BigDecimal("2100.550000000000000000"), got.price().value());
					assertFalse(got.enabled());
					assertEquals(t2, got.updatedAt());
				})
				.verifyComplete();

		// and
		StepVerifier.create(repository.findByMarketId(id.toString()))
				.assertNext(market -> {
					assertEquals("ETH", market.getBaseToken());
					assertEquals("USDT", market.getQuoteToken());
				})
				.verifyComplete();
	}

	@Test
	void findByIdReturnsEmptyWhenAbsent() {
		MarketId id = id("DOGE", "EUR");

		StepVerifier.create(adapter.findById(id))
				.expectNext()
				.verifyComplete();
	}

	@Test
	void findAllEnabledSinceReturnsOnlyEnabledAfterCutoff() {
		// given
		Instant t0 = Instant.parse("2025-01-01T10:00:00Z");
		MarketSnapshot m1 = snap(id("BTC", "USDT"), "30000", true, t0);
		MarketSnapshot m2 = snap(id("ETH", "USDT"), "2000", true, t0.minusSeconds(120));
		MarketSnapshot m3 = snap(id("ADA", "USDT"), "1.23", false, t0.plusSeconds(10));

		Instant cutoff = t0.minusSeconds(60);

		var probe = adapter.upsert(m1)
				.then(adapter.upsert(m2))
				.then(adapter.upsert(m3))
				.thenMany(adapter.findAllEnabledSince(cutoff))
				.collectList();

		// then
		StepVerifier.create(probe)
				.assertNext(list -> {
					assertEquals(1, list.size());
					MarketSnapshot got = list.getFirst();

					assertTrue(got.enabled());
					assertTrue(got.updatedAt().isAfter(cutoff));

					assertEquals("BTC", got.marketId().base().value());
					assertEquals("USDT", got.marketId().quote().value());
					assertEquals(new BigDecimal("30000.000000000000000000"), got.price().value());
				})
				.verifyComplete();
	}


	private MarketId id(String base, String quote) {
		return new MarketId(new Token(base), new Token(quote));
	}

	private MarketSnapshot snap(MarketId id, String price, boolean enabled, Instant updatedAt) {
		return new MarketSnapshot(id, new Price(new BigDecimal(price)), enabled, updatedAt);
	}
}
