package com.punks.recruitment.infrastructure.adapter.out.persistence;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.punks.recruitment.config.AbstractR2dbcPostgresIT;
import com.punks.recruitment.config.ImportTestConfig;
import com.punks.recruitment.domain.convert.ConversionId;
import com.punks.recruitment.domain.id.CommandId;
import com.punks.recruitment.domain.money.Amount;
import com.punks.recruitment.domain.money.Rate;
import com.punks.recruitment.domain.route.*;
import com.punks.recruitment.infrastructure.adapter.out.persistence.entities.OutboxEventEntity;
import com.punks.recruitment.infrastructure.adapter.out.persistence.repositories.ReactiveOutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Import(OutboxPortAdapter.class)
@ImportTestConfig
class OutboxPortAdapterTestIT extends AbstractR2dbcPostgresIT {

	@Autowired
	ReactiveOutboxRepository repo;

	@Autowired
	OutboxPortAdapter adapter;

	@Autowired
	ObjectMapper objectMapper;

	@BeforeEach
	void cleanDatabase() {
		StepVerifier.create(repo.deleteAll()).verifyComplete();
	}

	@Test
	void appendPersistsOutboxEntityWithSerializedPayload() {
		// given
		TokenConvertedEvent event = sampleTokenConvertedEvent();

		// when & then
		StepVerifier.create(adapter.append(event)
						.thenMany(repo.findAll().collectList()))
				.assertNext(list -> {
					assertEquals(1, list.size());
					OutboxEventEntity entity = list.getFirst();

					assertNotNull(entity.getEventId());
					assertEquals("Conversion", entity.getAggregateType());
					assertEquals("TokenConvertedEvent", entity.getEventType());
					assertEquals(event.conversionId().value().toString(), entity.getAggregateId());

					JsonNode root = parseJson(entity.getPayloadJson().asString());

					assertEquals("BTC", textAt(root, "tokenFrom.value"));
					assertEquals("USDT", textAt(root, "tokenTo.value"));
					assertEquals(15000, intAt(root, "amountOut.value"));
				})
				.verifyComplete();
	}

	private TokenConvertedEvent sampleTokenConvertedEvent() {
		ConversionId convId = new ConversionId();
		CommandId cmdId = new CommandId();
		Token from = new Token("BTC");
		Token to = new Token("USDT");
		Amount in = new Amount(new BigDecimal("0.5"));
		Amount out = new Amount(new BigDecimal("15000"));
		Instant quotedAt = Instant.parse("2024-01-01T00:00:00Z");

		Hop hop = new Hop(from, to, new Rate(new BigDecimal("30000")), quotedAt);
		Route route = new Route(List.of(hop));

		return new TokenConvertedEvent(
				convId, cmdId, from, to, in, out, route, quotedAt
		);
	}

	private JsonNode parseJson(String json) {
		try {
			return objectMapper.readTree(json);
		} catch (Exception e) {
			fail("Failed to parse JSON: " + e.getMessage());
			return null;
		}
	}

	private String textAt(JsonNode root, String path) {
		return resolvePath(root, path).asText();
	}

	private int intAt(JsonNode root, String path) {
		return resolvePath(root, path).asInt();
	}

	private JsonNode resolvePath(JsonNode root, String path) {
		String[] parts = path.split("\\.");
		JsonNode node = root;
		for (String p : parts) {
			node = node.path(p);
		}
		return node;
	}
}
