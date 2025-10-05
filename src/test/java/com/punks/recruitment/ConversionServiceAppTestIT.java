package com.punks.recruitment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.punks.recruitment.domain.route.TokenConvertedEvent;
import com.punks.recruitment.infrastructure.adapter.in.kafka.message.ConvertTokenMessage;
import com.punks.recruitment.infrastructure.adapter.in.kafka.message.MarketEventMessage;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(
		classes = ConversionServiceApp.class,
		webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@Testcontainers
@ActiveProfiles("test")
class ConversionServiceAppTestIT {

	@Container
	static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
			.withDatabaseName("conversion")
			.withUsername("conversion")
			.withPassword("conversion");

	@Container
	static KafkaContainer kafka =
			new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.0"));

	@DynamicPropertySource
	static void registerProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.r2dbc.url", () ->
				String.format("r2dbc:postgresql://%s:%d/%s",
						postgres.getHost(),
						postgres.getMappedPort(5432),
						postgres.getDatabaseName())
		);
		registry.add("spring.r2dbc.username", postgres::getUsername);
		registry.add("spring.r2dbc.password", postgres::getPassword);
		registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
	}

	@Autowired
	KafkaTemplate<String, String> kafkaTemplate;

	@Autowired
	ObjectMapper om;

	static final String MARKET_EVENTS_TOPIC = "market-events";
	static final String CONVERT_COMMANDS_TOPIC = "convert-commands";
	static final String CONVERSION_EVENTS_TOPIC = "conversion-events";

	@Test
	void shouldConvertTokensWhenMarketEnabled() throws Exception {
		// given
		var now = Instant.now();

		var marketEvent = new MarketEventMessage(
				"MarketPriceChanged",
				"BTC-USDT",
				BigDecimal.valueOf(60000),
				now,
				"changing price..."
		);

		var convertCommand = new ConvertTokenMessage(
				UUID.randomUUID(),
				"idempotency-key",
				"BTC",
				"USDT",
				BigDecimal.ONE,
				now
		);

		var consumer = createTestConsumer(CONVERSION_EVENTS_TOPIC);

		kafkaTemplate.send(MARKET_EVENTS_TOPIC, om.writeValueAsString(marketEvent));
		TimeUnit.SECONDS.sleep(10);
		kafkaTemplate.send(CONVERT_COMMANDS_TOPIC, om.writeValueAsString(convertCommand));

		// then
		await()
				.atMost(10, TimeUnit.SECONDS)
				.pollInterval(Duration.ofSeconds(1))
				.untilAsserted(() -> {
					ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
					for (ConsumerRecord<String, String> record : records) {
						TokenConvertedEvent event = om.readValue(record.value(), TokenConvertedEvent.class);
						System.out.println("Received conversion event: " + event);

						assertThat(event.tokenFrom().value()).isEqualTo("BTC");
						assertThat(event.tokenTo().value()).isEqualTo("USDT");
						assertThat(event.amountIn().value()).isEqualTo(BigDecimal.ONE);
						assertThat(event.amountOut().value()).isGreaterThan(BigDecimal.ZERO);
						return; // success
					}
					throw new AssertionError("No TokenConvertedEvent received yet");
				});

		consumer.close();
	}

	private KafkaConsumer<String, String> createTestConsumer(String topic) {
		var props = new Properties();
		props.put("bootstrap.servers", kafka.getBootstrapServers());
		props.put("group.id", "test-group-" + UUID.randomUUID());
		props.put("auto.offset.reset", "earliest");
		props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
		props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");

		var consumer = new KafkaConsumer<String, String>(props);
		consumer.subscribe(Collections.singletonList(topic));
		return consumer;
	}
}
