package com.punks.recruitment.application.service.convert.mapper;

import com.punks.recruitment.application.service.convert.dto.ConversionResult;
import com.punks.recruitment.domain.convert.ConversionRecord;

public final class ConversionMapper {

	private ConversionMapper() {
	}

	public static ConversionResult toResult(ConversionRecord conversionRecord) {
		return new ConversionResult(
				conversionRecord.conversionId(),
				conversionRecord.commandId(),
				conversionRecord.tokenFrom(),
				conversionRecord.tokenTo(),
				conversionRecord.amountIn(),
				conversionRecord.amountOut(),
				conversionRecord.route(),
				conversionRecord.computedAt()
		);
	}
}
