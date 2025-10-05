package com.punks.recruitment.application.port.in.convert;

import com.punks.recruitment.application.service.convert.dto.ConversionResult;
import com.punks.recruitment.domain.convert.ConvertCommand;
import reactor.core.publisher.Mono;

public interface ConvertTokenUseCase {
	Mono<ConversionResult> convert(ConvertCommand command);
}
