package com.punks.recruitment.application.port.out.outbox;

import com.punks.recruitment.domain.route.TokenConvertedEvent;
import reactor.core.publisher.Mono;

public interface OutboxPort {
	Mono<Void> append(TokenConvertedEvent event);
}
