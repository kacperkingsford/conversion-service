package com.punks.recruitment.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.punks.recruitment.application.port.out.clock.ClockPort;
import com.punks.recruitment.infrastructure.common.serializer.DomainEventSerializer;
import com.punks.recruitment.infrastructure.common.serializer.jackson.JacksonDomainEventSerializer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.time.Instant;

@TestConfiguration
public class TestBeansConfig {

	@Bean
	ObjectMapper testObjectMapper() {
		ObjectMapper om = new ObjectMapper();
		om.registerModule(new JavaTimeModule());
		om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		return om;
	}

	@Bean
	DomainEventSerializer testDomainEventSerializer(ObjectMapper om) {
		return new JacksonDomainEventSerializer(om);
	}

	@Bean
	ClockPort testClockPort() {
		return () -> Instant.parse("2024-01-01T00:00:00Z");
	}
}
