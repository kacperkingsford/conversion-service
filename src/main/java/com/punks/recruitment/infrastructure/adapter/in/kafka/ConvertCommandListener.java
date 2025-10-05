package com.punks.recruitment.infrastructure.adapter.in.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.punks.recruitment.application.port.in.convert.ConvertTokenUseCase;
import com.punks.recruitment.application.service.convert.dto.ConversionResult;
import com.punks.recruitment.infrastructure.adapter.in.kafka.message.ConvertTokenMessage;
import com.punks.recruitment.infrastructure.adapter.in.kafka.message.InboundMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
public class ConvertCommandListener {

	private final ConvertTokenUseCase convertUseCase;
	private final ObjectMapper om;

	public ConvertCommandListener(ConvertTokenUseCase convertUseCase, ObjectMapper om) {
		this.convertUseCase = convertUseCase;
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
			topics = "${topics.convert-commands}",
			groupId = "${spring.kafka.consumer.group-id}"
	)
	public void onMessage(
			ConsumerRecord<String, String> rec,
			Acknowledgment ack,
			@Header(name = KafkaHeaders.RECEIVED_KEY, required = false) String key
	) throws JsonProcessingException {

		log.debug("ConvertCommand IN key={} partition={} offset={}", key, rec.partition(), rec.offset());

		try {
			var message = parseMessage(rec);
			processMessage(message, rec, key);
			acknowledge(ack, rec, key);
		} catch (JsonProcessingException | IllegalArgumentException nonRetryable) {
			handleNonRetryable(rec, key, nonRetryable);
			throw nonRetryable;
		} catch (RuntimeException retryable) {
			handleRetryable(rec, key, retryable);
			throw retryable;
		}
	}

	private ConvertTokenMessage parseMessage(ConsumerRecord<String, String> rec) throws JsonProcessingException {
		return om.readValue(rec.value(), ConvertTokenMessage.class);
	}

	private void processMessage(ConvertTokenMessage message, ConsumerRecord<String, String> rec, String key) {
		var cmd = InboundMapper.toDomain(message);
		Mono<ConversionResult> pipeline =
				convertUseCase.convert(cmd)
						.doOnSuccess(__ -> log.debug("ConvertCommand succeeded, key={} offset={}", key, rec.offset()))
						.doOnError(err -> log.warn("ConvertCommand retryable key={} offset={}", key, rec.offset(), err));
		pipeline.block();
	}

	private void acknowledge(Acknowledgment ack, ConsumerRecord<String, String> rec, String key) {
		ack.acknowledge();
		log.debug("ConvertCommand ACK key={} offset={}", key, rec.offset());
	}

	private void handleNonRetryable(ConsumerRecord<String, String> rec, String key, Exception ex) {
		log.warn("ConvertCommand non-retryable key={} offset={}", key, rec.offset(), ex);
	}

	private void handleRetryable(ConsumerRecord<String, String> rec, String key, Exception ex) {
		log.warn("ConvertCommand retryable key={} offset={}", key, rec.offset(), ex);
	}

	@KafkaListener(
			topics = "${topics.convert-commands}-dlq",
			groupId = "${spring.kafka.consumer.group-id}"
	)
	public void onDltMessage(String payload) {
		log.error("DLT convert-commands received: {}", payload); // TODO handle DLT
	}
}
