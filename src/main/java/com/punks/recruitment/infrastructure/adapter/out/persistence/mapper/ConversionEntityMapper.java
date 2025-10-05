package com.punks.recruitment.infrastructure.adapter.out.persistence.mapper;

import com.punks.recruitment.domain.convert.ConversionId;
import com.punks.recruitment.domain.convert.ConversionRecord;
import com.punks.recruitment.domain.convert.ConversionStatus;
import com.punks.recruitment.domain.id.CommandId;
import com.punks.recruitment.domain.id.IdempotencyKey;
import com.punks.recruitment.domain.money.Amount;
import com.punks.recruitment.domain.route.Route;
import com.punks.recruitment.domain.route.Token;
import com.punks.recruitment.infrastructure.adapter.out.persistence.entities.ConversionEntity;
import io.r2dbc.postgresql.codec.Json;

import java.util.Optional;

public class ConversionEntityMapper {

	private ConversionEntityMapper() {
	}

	public static ConversionEntity toEntity(ConversionRecord record, String routeJson) {
		ConversionEntity e = new ConversionEntity();
		e.setCommandId(record.commandId().value());
		e.setIdempotencyKey(record.idempotencyKey().map(IdempotencyKey::value).orElse(null));
		e.setTokenFrom(record.tokenFrom().value());
		e.setTokenTo(record.tokenTo().value());
		e.setAmountIn(record.amountIn().value());
		e.setAmountOut(record.amountOut().value());
		e.setComputedAt(record.computedAt());
		e.setStatus(record.status().toString());
		e.setPathJson(Json.of(routeJson));
		return e;
	}

	public static ConversionRecord toDomain(ConversionEntity entity, Route route) {
		return new ConversionRecord(
				new ConversionId(entity.getConversionId()),
				new CommandId(entity.getCommandId()),
				Optional.ofNullable(entity.getIdempotencyKey()).map(IdempotencyKey::new),
				new Token(entity.getTokenFrom()),
				new Token(entity.getTokenTo()),
				new Amount(entity.getAmountIn()),
				new Amount(entity.getAmountOut()),
				route,
				entity.getComputedAt(),
				ConversionStatus.valueOf(entity.getStatus())
		);
	}
}
