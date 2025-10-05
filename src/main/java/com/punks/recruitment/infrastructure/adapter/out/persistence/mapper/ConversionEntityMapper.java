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

	public static ConversionEntity toEntity(ConversionRecord conversionRecord, String routeJson) {
		ConversionEntity entity = ConversionEntity.builder()
				.conversionId(conversionRecord.conversionId().value())
				.commandId(conversionRecord.commandId().value())
				.idempotencyKey(conversionRecord.idempotencyKey().map(IdempotencyKey::value).orElse(null))
				.tokenFrom(conversionRecord.tokenFrom().value())
				.tokenTo(conversionRecord.tokenTo().value())
				.amountIn(conversionRecord.amountIn().value())
				.amountOut(conversionRecord.amountOut().value())
				.computedAt(conversionRecord.computedAt())
				.status(conversionRecord.status().toString())
				.pathJson(Json.of(routeJson))
				.build();
		entity.markAsNew();
		return entity;
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
