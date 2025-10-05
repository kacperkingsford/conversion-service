package com.punks.recruitment.infrastructure.adapter.out.kafka;

import com.punks.recruitment.application.port.out.clock.ClockPort;
import com.punks.recruitment.application.port.out.outbox.OutboxPollingPort;
import com.punks.recruitment.application.port.out.outbox.OutboxRecord;
import com.punks.recruitment.config.properties.TopicsProperties;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class OutboxScheduler {

	private static final Logger log = LoggerFactory.getLogger(OutboxScheduler.class);

	private final OutboxPollingPort port;
	private final ClockPort clock;
	private final KafkaTemplate<String, String> kafka;
	private final TopicsProperties topics;
	private final int batchSize;

	public OutboxScheduler(OutboxPollingPort port,
						   ClockPort clock,
						   KafkaTemplate<String, String> kafka,
						   TopicsProperties topics,
						   @Value("${outbox.batch-size:50}") int batchSize) {
		this.port = port;
		this.clock = clock;
		this.kafka = kafka;
		this.topics = topics;
		this.batchSize = batchSize;
	}

	@Scheduled(fixedDelayString = "${outbox.poll-interval:500ms}")
	@SchedulerLock(name = "outbox-publisher", lockAtLeastFor = "PT0.1S", lockAtMostFor = "PT30S")
	// TODO can be moved to config as well
	public void publishOutboxBatch() {
		port.fetchUnpublished(batchSize)
				.collectList()
				.flatMapMany(list -> {
					if (list.isEmpty()) {
						log.debug("Outbox tick: no unpublished events.");
					} else {
						log.info("Outbox tick: fetched {} unpublished events.", list.size());
					}
					return Flux.fromIterable(list);
				})
				.concatMap(this::publishOne)
				.doOnError(ex -> log.warn("Outbox batch failed: {}", ex.toString()))
				.subscribe();
	}

	private Mono<Void> publishOne(OutboxRecord outboxRecord) {
		String topic = topics.getConversionEvents();
		String key = outboxRecord.eventId().toString();
		String value = outboxRecord.payloadJson();

		ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, value);

		log.debug("Publishing outbox event id={} to topic={}", key, topic);

		return Mono.fromFuture(kafka.send(record))
				.doOnSuccess(sendResult -> {
					RecordMetadata md = sendResult.getRecordMetadata();
					log.debug("Kafka sent id={} topic={} partition={} offset={}", key, md.topic(), md.partition(), md.offset());
				})
				.then(port.markPublished(outboxRecord.eventId(), clock.now()))
				.doOnSuccess(v -> log.debug("Marked published id={}", key))
				.doOnError(ex -> log.warn("Kafka publish failed id={} : {}", key, ex.toString()));
	}
}
