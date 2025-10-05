package com.punks.recruitment.config.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.punks.recruitment.application.port.out.clock.ClockPort;
import com.punks.recruitment.application.port.out.precision.PrecisionPolicyPort;
import com.punks.recruitment.config.properties.PrecisionProperties;
import com.punks.recruitment.domain.route.Token;
import com.punks.recruitment.infrastructure.common.deserializer.DomainPayloadDeserializer;
import com.punks.recruitment.infrastructure.common.deserializer.jackson.JacksonDomainPayloadDeserializer;
import com.punks.recruitment.infrastructure.common.serializer.DomainEventSerializer;
import com.punks.recruitment.infrastructure.common.serializer.jackson.JacksonDomainEventSerializer;
import io.r2dbc.spi.ConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.r2dbc.connection.R2dbcTransactionManager;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.reactive.TransactionalOperator;

import java.math.MathContext;
import java.time.Instant;

@Configuration
public class AppConfig {
	@Bean
	public TransactionalOperator transactionalOperator(ReactiveTransactionManager txManager) {
		return TransactionalOperator.create(txManager);
	}

	@Bean
	public ReactiveTransactionManager r2dbcTxManager(ConnectionFactory cf) {
		return new R2dbcTransactionManager(cf);
	}

	@Bean
	ObjectMapper objectMapper() {
		ObjectMapper om = new ObjectMapper();
		om.registerModule(new JavaTimeModule());
		return om.findAndRegisterModules();
	}

	@Bean
	public ClockPort clockPort() {
		return Instant::now; // TODO can be changed
	}

	@Bean
	public PrecisionPolicyPort precisionPolicy(PrecisionProperties properties) {
		return new PrecisionPolicyPort() {
			@Override
			public MathContext mathContext() {
				return new MathContext(properties.getContext());
			}

			@Override
			public int scaleFor(Token token) {
				return properties.getScale();
			}
		};
	}

	@Bean
	public DomainEventSerializer domainEventSerializer(ObjectMapper om) {
		return new JacksonDomainEventSerializer(om);
	}

	@Bean
	public DomainPayloadDeserializer domainPayloadDeserializer(ObjectMapper om) {
		return new JacksonDomainPayloadDeserializer(om);
	}

}
