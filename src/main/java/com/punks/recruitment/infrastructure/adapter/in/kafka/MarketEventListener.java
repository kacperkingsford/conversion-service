package com.punks.recruitment.infrastructure.adapter.in.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.punks.recruitment.application.port.in.market.MarketUpdateUseCase;
import com.punks.recruitment.infrastructure.adapter.in.kafka.message.InboundMapper;
import com.punks.recruitment.infrastructure.adapter.in.kafka.message.MarketEventMessage;
import com.punks.recruitment.infrastructure.adapter.in.kafka.message.exception.InvalidMarketEventTypeException;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class MarketEventListener {

	private final MarketUpdateUseCase marketUpdateUseCase;
	private final ObjectMapper om;

	public MarketEventListener(MarketUpdateUseCase marketUpdateUseCase, ObjectMapper om) {
		this.marketUpdateUseCase = marketUpdateUseCase;
		this.om = om;
	}

	@RetryableTopic(
			attempts = "${kafka.retry.attempts}",
			backoff = @Backoff(
					delayExpression = "${kafka.retry.backoff.delay}",
					multiplierExpression = "${kafka.retry.backoff.multiplier}"
			),
			dltTopicSuffix = "-dlq",
			exclude = {JsonProcessingException.class, IllegalArgumentException.class}
	)
	@KafkaListener(
			topics = "${topics.market-events}",
			groupId = "${spring.kafka.consumer.group-id}"
	)
	public void onMessage(
			ConsumerRecord<String, String> rec,
			Acknowledgment ack,
			@Header(name = KafkaHeaders.RECEIVED_KEY, required = false) String key
	) throws JsonProcessingException {

		log.debug("MarketEvent IN key={} partition={} offset={}", key, rec.partition(), rec.offset());

		try {
			var msg = parseMessage(rec);
			processMessage(msg, rec, key);
			acknowledge(ack, rec, key, msg);
		} catch (JsonProcessingException | IllegalArgumentException nonRetryable) {
			handleNonRetryable(rec, key, nonRetryable);
			throw nonRetryable;
		} catch (RuntimeException retryable) {
			handleRetryable(rec, key, retryable);
			throw retryable;
		}
	}

	private MarketEventMessage parseMessage(ConsumerRecord<String, String> rec) throws JsonProcessingException {
		return om.readValue(rec.value(), MarketEventMessage.class);
	}

	private void processMessage(MarketEventMessage msg, ConsumerRecord<String, String> rec, String key) {
		Mono<Void> work = switch (msg.type()) {
			// TODO in proto would be enum type
			case "MarketPriceChanged" -> marketUpdateUseCase.onPriceChanged(InboundMapper.toDomainPrice(msg));
			case "MarketEnabled" -> marketUpdateUseCase.onMarketEnabled(InboundMapper.toDomainStatus(msg));
			case "MarketDisabled" -> marketUpdateUseCase.onMarketDisabled(InboundMapper.toDomainStatus(msg));
			default -> Mono.error(new InvalidMarketEventTypeException(msg.type()));
		};

		work.doOnSuccess(__ -> log.info("MarketEvent OK type={} key={} offset={}", msg.type(), key, rec.offset()))
				.doOnError(err -> log.error("MarketEvent retryable type={} key={} offset={} -> {}", msg.type(), key, rec.offset(), err.toString()))
				.block();
	}

	private void acknowledge(Acknowledgment ack, ConsumerRecord<String, String> rec, String key, MarketEventMessage msg) {
		ack.acknowledge();
		log.debug("MarketEvent ACK type={} key={} offset={}", msg.type(), key, rec.offset());
	}

	private void handleNonRetryable(ConsumerRecord<String, String> rec, String key, Exception ex) {
		log.error("MarketEvent non-retryable key={} offset={}", key, rec.offset(), ex);
	}

	private void handleRetryable(ConsumerRecord<String, String> rec, String key, Exception ex) {
		log.error("MarketEvent retryable key={} offset={}", key, rec.offset(), ex);
	}

	@KafkaListener(
			topics = "${topics.market-events}-dlq",
			groupId = "${spring.kafka.consumer.group-id}"
	)
	public void onDltMessage(String payload) {
		log.error("DLT market-events received: {}", payload); // TODO handle DLT
	}
}
