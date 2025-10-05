package com.punks.recruitment.domain.route;

import com.punks.recruitment.domain.money.Amount;
import com.punks.recruitment.domain.id.CommandId;
import com.punks.recruitment.domain.convert.ConversionId;

import java.time.Instant;

public record TokenConvertedEvent(
		ConversionId conversionId,
		CommandId commandId,
		Token tokenFrom,
		Token tokenTo,
		Amount amountIn,
		Amount amountOut,
		Route route,
		Instant quotedAt
) {
}
