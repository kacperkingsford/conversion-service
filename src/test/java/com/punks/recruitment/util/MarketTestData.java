package com.punks.recruitment.util;

import com.punks.recruitment.infrastructure.adapter.in.kafka.message.ConvertTokenMessage;
import com.punks.recruitment.infrastructure.adapter.in.kafka.message.MarketEventMessage;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class MarketTestData {

	public static MarketEventMessage buildMarketPriceChangedMessage(String pair, BigDecimal price, String desc) {
		return new MarketEventMessage(
				"MarketPriceChanged",
				pair,
				price,
				Instant.now(),
				desc
		);
	}

	public static ConvertTokenMessage buildConvertTokenMessage(String from, String to, BigDecimal amount) {
		return new ConvertTokenMessage(
				UUID.randomUUID(),
				"idempotency-" + UUID.randomUUID(),
				from,
				to,
				amount,
				Instant.now()
		);
	}
}
