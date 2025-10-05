package com.punks.recruitment.domain.convert;

import com.punks.recruitment.domain.id.CommandId;
import com.punks.recruitment.domain.id.IdempotencyKey;
import com.punks.recruitment.domain.money.Amount;
import com.punks.recruitment.domain.route.Route;
import com.punks.recruitment.domain.route.Token;
import com.punks.recruitment.domain.route.TokenConvertedEvent;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public record ConversionRecord(
		ConversionId conversionId,
		CommandId commandId,
		Optional<IdempotencyKey> idempotencyKey,
		Token tokenFrom,
		Token tokenTo,
		Amount amountIn,
		Amount amountOut,
		Route route,
		Instant computedAt,
		ConversionStatus status
) {
	public static ConversionRecord succeeded(ConvertCommand cmd, Amount amountOut, Route route, Instant computedAt) {
		return new ConversionRecord(
				new ConversionId(),
				cmd.commandId(),
				cmd.idempotencyKey(),
				cmd.tokenFrom(),
				cmd.tokenTo(),
				cmd.amount(),
				amountOut,
				route,
				computedAt,
				ConversionStatus.SUCCEEDED
		);
	}

	public static ConversionRecord failedNoRoute(ConvertCommand cmd, Instant computedAt) {
		return new ConversionRecord(
				new ConversionId(),
				cmd.commandId(),
				cmd.idempotencyKey(),
				cmd.tokenFrom(),
				cmd.tokenTo(),
				cmd.amount(),
				new Amount(BigDecimal.ONE),
				new Route(List.of()),
				computedAt,
				ConversionStatus.NO_ROUTE
		);
	}

	public TokenConvertedEvent toEvent() {
		return new TokenConvertedEvent(
				conversionId,
				commandId,
				tokenFrom,
				tokenTo,
				amountIn,
				amountOut,
				route,
				computedAt
		);
	}

}
