package com.punks.recruitment.infrastructure.adapter.out.persistence;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.punks.recruitment.config.AbstractR2dbcPostgresIT;
import com.punks.recruitment.application.port.out.convert.ConversionRepository;
import com.punks.recruitment.domain.convert.ConversionId;
import com.punks.recruitment.domain.convert.ConversionRecord;
import com.punks.recruitment.domain.convert.ConversionStatus;
import com.punks.recruitment.domain.id.CommandId;
import com.punks.recruitment.domain.id.IdempotencyKey;
import com.punks.recruitment.domain.money.Amount;
import com.punks.recruitment.domain.money.Rate;
import com.punks.recruitment.domain.route.Hop;
import com.punks.recruitment.domain.route.Route;
import com.punks.recruitment.domain.route.Token;
import com.punks.recruitment.infrastructure.adapter.out.persistence.repositories.ReactiveConversionRepository;
import com.punks.recruitment.infrastructure.common.deserializer.DomainPayloadDeserializer;
import com.punks.recruitment.infrastructure.common.serializer.DomainEventSerializer;
import com.punks.recruitment.infrastructure.common.serializer.jackson.JacksonDomainEventSerializer;
import com.punks.recruitment.infrastructure.common.deserializer.jackson.JacksonDomainPayloadDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@Import({ConversionRepositoryAdapterTestIT.TestConfig.class, ConversionRepositoryAdapter.class})
class ConversionRepositoryAdapterTestIT extends AbstractR2dbcPostgresIT {

	@Autowired
	ConversionRepository adapter;
	@Autowired
	ReactiveConversionRepository repo;

	@BeforeEach
	void clean() {
		StepVerifier.create(repo.deleteAll()).verifyComplete();
	}

	@Test
	void saveIfAbsentInsertsAndRoundTripsRouteJson() {
		// given
		ConversionRecord record = record(
				new ConversionId(),
				new CommandId(),
				Optional.of(new IdempotencyKey(UUID.randomUUID().toString())),
				new Token("BTC"),
				new Token("USDT"),
				new Amount(new BigDecimal("0.5")),
				new Amount(new BigDecimal("15000")),
				route(singleHop("BTC", "USDT", "30000", "2024-01-01T00:00:00Z")),
				Instant.parse("2024-01-01T00:00:05Z"),
				ConversionStatus.SUCCEEDED
		);

		// when
		Mono<ConversionRecord> probe = adapter.saveIfAbsent(record);

		// then
		StepVerifier.create(probe)
				.assertNext(saved -> {
					assertEquals(record.commandId(), saved.commandId());
					assertEquals(record.tokenFrom().value(), saved.tokenFrom().value());
					assertEquals(record.tokenTo().value(), saved.tokenTo().value());
					assertEquals(record.amountOut().value(), saved.amountOut().value());

					assertEquals(1, saved.route().hops().size());
					var hop = saved.route().hops().getFirst();
					assertEquals("BTC", hop.from().value());
					assertEquals("USDT", hop.to().value());
					assertEquals(new BigDecimal("30000"), hop.rate().value());
				})
				.verifyComplete();

		// and
		StepVerifier.create(repo.findByCommandId(record.commandId().value()))
				.assertNext(e -> assertEquals(record.commandId().value(), e.getCommandId()))
				.verifyComplete();
	}

	@Test
	void saveIfAbsentDuplicateByCommandReturnsExistingRecord() {
		// given:
		CommandId cmd = new CommandId();
		ConversionRecord first = record(
				new ConversionId(),
				cmd,
				Optional.empty(),
				new Token("ETH"), new Token("USDT"),
				new Amount(new BigDecimal("1.0")),
				new Amount(new BigDecimal("2100")),
				route(singleHop("ETH", "USDT", "2100", "2025-01-01T00:00:00Z")),
				Instant.parse("2025-01-01T00:00:01Z"),
				ConversionStatus.SUCCEEDED
		);

		ConversionRecord second = record(
				new ConversionId(),
				cmd,
				Optional.of(new IdempotencyKey(UUID.randomUUID().toString())),
				new Token("ETH"), new Token("USDT"),
				new Amount(new BigDecimal("2.0")),
				new Amount(new BigDecimal("4200")),
				route(singleHop("ETH", "USDT", "2100", "2025-01-01T00:00:00Z")),
				Instant.parse("2025-01-01T00:00:02Z"),
				ConversionStatus.SUCCEEDED
		);

		// when
		Mono<ConversionRecord> flow = adapter.saveIfAbsent(first)
				.then(adapter.saveIfAbsent(second));

		// then
		StepVerifier.create(flow)
				.assertNext(returned -> assertEquals(first.commandId(), returned.commandId()))
				.verifyComplete();

		// and
		StepVerifier.create(repo.findByCommandId(cmd.value()))
				.assertNext(e -> assertEquals(first.commandId().value(), e.getCommandId()))
				.verifyComplete();
	}

	@Test
	void saveIfAbsentDuplicateByIdempotencyReturnsExistingRecord() {
		// given
		IdempotencyKey key = new IdempotencyKey(UUID.randomUUID().toString());

		ConversionRecord first = record(
				new ConversionId(),
				new CommandId(),
				Optional.of(key),
				new Token("SOL"), new Token("USDT"),
				new Amount(new BigDecimal("10")),
				new Amount(new BigDecimal("1000")),
				route(singleHop("SOL", "USDT", "100", "2024-06-01T00:00:00Z")),
				Instant.parse("2024-06-01T00:00:01Z"),
				ConversionStatus.SUCCEEDED
		);

		ConversionRecord second = record(
				new ConversionId(),
				new CommandId(),
				Optional.of(key),
				new Token("SOL"), new Token("USDT"),
				new Amount(new BigDecimal("20")),
				new Amount(new BigDecimal("2000")),
				route(singleHop("SOL", "USDT", "100", "2024-06-01T00:00:00Z")),
				Instant.parse("2024-06-01T00:00:02Z"),
				ConversionStatus.SUCCEEDED
		);

		Mono<ConversionRecord> flow = adapter.saveIfAbsent(first)
				.then(adapter.saveIfAbsent(second));

		StepVerifier.create(flow)
				.assertNext(returned -> assertEquals(first.commandId(), returned.commandId()))
				.verifyComplete();

		StepVerifier.create(repo.findByIdempotencyKey(key.value()))
				.assertNext(e -> assertEquals(first.commandId().value(), e.getCommandId()))
				.verifyComplete();
	}

	@Test
	void findByCommandReturnsPresentWhenExists() {
		CommandId cmd = new CommandId();
		ConversionRecord rec = record(
				new ConversionId(), cmd, Optional.empty(),
				new Token("BTC"), new Token("ETH"),
				new Amount(new BigDecimal("0.1")),
				new Amount(new BigDecimal("2.0")),
				route(List.of(
						hop("BTC", "USDT", "30000", "2024-01-01T00:00:00Z"),
						hop("USDT", "ETH", "0.00005", "2024-01-01T00:00:00Z")
				)),
				Instant.parse("2024-01-01T00:00:10Z"),
				ConversionStatus.SUCCEEDED
		);

		StepVerifier.create(adapter.saveIfAbsent(rec).then(adapter.findByCommand(cmd)))
				.assertNext(opt -> {
					assertTrue(opt.isPresent());
					ConversionRecord got = opt.get();
					assertEquals(cmd, got.commandId());
					assertEquals(2, got.route().hops().size());
				})
				.verifyComplete();
	}

	@Test
	void findByIdempotency_returnsEmpty_whenMissing() {
		IdempotencyKey key = new IdempotencyKey("nope");
		StepVerifier.create(adapter.findByIdempotency(key))
				.assertNext(opt -> assertFalse(opt.isPresent()))
				.verifyComplete();
	}

	static class TestConfig {
		@Bean
		ObjectMapper testObjectMapper() {
			ObjectMapper om = new ObjectMapper();
			om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
			om.registerModule(new JavaTimeModule());
			return om;
		}

		@Bean
		DomainEventSerializer testSerializer(ObjectMapper om) {
			return new JacksonDomainEventSerializer(om);
		}

		@Bean
		DomainPayloadDeserializer testDeserializer(ObjectMapper om) {
			return new JacksonDomainPayloadDeserializer(om);
		}
	}

	private static Route route(List<Hop> hops) {
		return new Route(hops);
	}

	private static List<Hop> singleHop(String from, String to, String rate, String quotedAtIso) {
		return List.of(hop(from, to, rate, quotedAtIso));
	}

	private static Hop hop(String from, String to, String rate, String quotedAtIso) {
		return new Hop(new Token(from), new Token(to), new Rate(new BigDecimal(rate)), Instant.parse(quotedAtIso));
	}

	private static ConversionRecord record(ConversionId conversionId,
										   CommandId commandId,
										   Optional<IdempotencyKey> idempotencyKey,
										   Token from,
										   Token to,
										   Amount in,
										   Amount out,
										   Route route,
										   Instant computedAt,
										   ConversionStatus status) {
		return new ConversionRecord(
				conversionId,
				commandId,
				idempotencyKey,
				from,
				to,
				in,
				out,
				route,
				computedAt,
				status
		);
	}
}
