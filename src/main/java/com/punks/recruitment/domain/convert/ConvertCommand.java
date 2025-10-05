package com.punks.recruitment.domain.convert;

import com.punks.recruitment.domain.money.Amount;
import com.punks.recruitment.domain.id.CommandId;
import com.punks.recruitment.domain.id.IdempotencyKey;
import com.punks.recruitment.domain.route.Token;

import java.time.Instant;
import java.util.Optional;

public record ConvertCommand(
		CommandId commandId,
		Optional<IdempotencyKey> idempotencyKey,
		Token tokenFrom,
		Token tokenTo,
		Amount amount,
		Instant requestedAt
) {
}
