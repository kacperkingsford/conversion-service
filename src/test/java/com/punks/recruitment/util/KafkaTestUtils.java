package com.punks.recruitment.util;

import org.apache.kafka.clients.consumer.KafkaConsumer;

import java.util.Collections;
import java.util.Properties;
import java.util.UUID;

public class KafkaTestUtils {

	public static KafkaConsumer<String, String> createTestConsumer(String bootstrapServers, String topic) {
		var props = new Properties();
		props.put("bootstrap.servers", bootstrapServers);
		props.put("group.id", "test-group-" + UUID.randomUUID());
		props.put("auto.offset.reset", "earliest");
		props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
		props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");

		var consumer = new KafkaConsumer<String, String>(props);
		consumer.subscribe(Collections.singletonList(topic));
		return consumer;
	}
}
