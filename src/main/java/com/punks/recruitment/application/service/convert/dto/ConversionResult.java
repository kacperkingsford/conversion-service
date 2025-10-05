package com.punks.recruitment.application.service.convert.dto;

import com.punks.recruitment.domain.convert.ConversionId;
import com.punks.recruitment.domain.money.Amount;
import com.punks.recruitment.domain.id.CommandId;
import com.punks.recruitment.domain.route.Route;
import com.punks.recruitment.domain.route.Token;

import java.time.Instant;

public record ConversionResult(
		ConversionId conversionId,
		CommandId commandId,
		Token tokenFrom,
		Token tokenTo,
		Amount amountIn,
		Amount amountOut,
		Route route,
		Instant computedAt
) {
}
