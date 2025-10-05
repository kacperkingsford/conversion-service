package com.punks.recruitment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.punks.recruitment.domain.route.TokenConvertedEvent;
import com.punks.recruitment.util.KafkaTestUtils;
import com.punks.recruitment.util.MarketTestData;
import com.punks.recruitment.infrastructure.adapter.out.persistence.entities.MarketEntity;
import com.punks.recruitment.infrastructure.adapter.out.persistence.repositories.ReactiveMarketRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.BeforeEach;
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
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(classes = ConversionServiceApp.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
@ActiveProfiles("test")
class ConversionServiceAppE2ETestIT {
	static final String MARKET_EVENTS_TOPIC = "market-events";
	static final String CONVERT_COMMANDS_TOPIC = "convert-commands";
	static final String CONVERSION_EVENTS_TOPIC = "conversion-events";

	@Autowired
	KafkaTemplate<String, String> kafkaTemplate;

	@Autowired
	ReactiveMarketRepository marketRepository;

	@Autowired
	ObjectMapper om;

	@Test
	void shouldConvertTokensViaIntermediateMarket() throws Exception {
		//given
		var marketBtcUsdt = MarketTestData.buildMarketPriceChangedMessage("BTC-USDT", BigDecimal.valueOf(60000), "BTC to USDT market");
		var marketEthUsdt = MarketTestData.buildMarketPriceChangedMessage("ETH-USDT", BigDecimal.valueOf(3000), "ETH to USDT market");
		var convertCommand = MarketTestData.buildConvertTokenMessage("BTC", "ETH", BigDecimal.ONE);

		var consumer = KafkaTestUtils.createTestConsumer(kafka.getBootstrapServers(), CONVERSION_EVENTS_TOPIC);

		// when
		kafkaTemplate.send(MARKET_EVENTS_TOPIC, om.writeValueAsString(marketBtcUsdt));
		kafkaTemplate.send(MARKET_EVENTS_TOPIC, om.writeValueAsString(marketEthUsdt));

		awaitMarketExists("BTC-USDT", "ETH-USDT");

		kafkaTemplate.send(CONVERT_COMMANDS_TOPIC, om.writeValueAsString(convertCommand));

		// then
		await()
				.atMost(Duration.ofSeconds(60))
				.untilAsserted(() -> {
					ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
					for (ConsumerRecord<String, String> record : records) {
						var event = om.readValue(record.value(), TokenConvertedEvent.class);
						assertThat(event.tokenFrom().value()).isEqualTo("BTC");
						assertThat(event.tokenTo().value()).isEqualTo("ETH");
						assertThat(event.amountOut().value()).isEqualByComparingTo(new BigDecimal("20.00000000"));
						return;
					}
					throw new AssertionError("No multi-hop TokenConvertedEvent received yet");
				});

		consumer.close();
	}

	@Test
	void shouldConvertTokensWhenMarketEnabledForDirectConversion() throws Exception {
		var marketEvent = MarketTestData.buildMarketPriceChangedMessage("BTC-USDT", BigDecimal.valueOf(60000), "changing price...");
		var convertCommand = MarketTestData.buildConvertTokenMessage("BTC", "USDT", BigDecimal.valueOf(2));

		var consumer = KafkaTestUtils.createTestConsumer(kafka.getBootstrapServers(), CONVERSION_EVENTS_TOPIC);

		// given
		kafkaTemplate.send(MARKET_EVENTS_TOPIC, om.writeValueAsString(marketEvent));

		awaitMarketExists("BTC-USDT");

		// when
		kafkaTemplate.send(CONVERT_COMMANDS_TOPIC, om.writeValueAsString(convertCommand));

		// then
		await()
				.atMost(Duration.ofSeconds(60))
				.untilAsserted(() -> {
					ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
					for (ConsumerRecord<String, String> record : records) {
						var event = om.readValue(record.value(), TokenConvertedEvent.class);
						assertThat(event.tokenFrom().value()).isEqualTo("BTC");
						assertThat(event.tokenTo().value()).isEqualTo("USDT");
						assertThat(event.amountOut().value()).isEqualByComparingTo(new BigDecimal("120000.00000000"));
						return;
					}
					throw new AssertionError("No TokenConvertedEvent received yet");
				});

		consumer.close();
	}

	private void awaitMarketExists(String... marketIds) {
		await()
				.atMost(Duration.ofSeconds(60))
				.pollInterval(Duration.ofSeconds(1))
				.untilAsserted(() -> {
					var markets = marketRepository.findAll().collectList().block();
					assertThat(markets)
							.extracting(MarketEntity::getMarketId)
							.contains(marketIds);
				});
	}

	// TODO move to common E2E test config
	@Container
	static PostgreSQLContainer<?> postgres =
			new PostgreSQLContainer<>("postgres:15").withDatabaseName("conversion").withUsername("conversion").withPassword("conversion");
	@Container
	static KafkaContainer kafka =
			new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.0"));

	@DynamicPropertySource
	static void registerProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.r2dbc.url", () ->
				String.format("r2dbc:postgresql://%s:%d/%s", postgres.getHost(), postgres.getMappedPort(5432), postgres.getDatabaseName()));
		registry.add("spring.r2dbc.username", postgres::getUsername);
		registry.add("spring.r2dbc.password", postgres::getPassword);
		registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
	}

	@BeforeEach
	void waitForKafkaReady() {
		await().atMost(30, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).untilAsserted(() -> {
			try (var producer = new KafkaProducer<String, String>(Map.of("bootstrap.servers", kafka.getBootstrapServers(), "key.serializer", "org.apache.kafka.common.serialization.StringSerializer", "value.serializer", "org.apache.kafka.common.serialization.StringSerializer"))) {
				producer.send(new ProducerRecord<>("__test_ready_check", "ping", "pong")).get(3, TimeUnit.SECONDS);
			}
		});
	}
}
