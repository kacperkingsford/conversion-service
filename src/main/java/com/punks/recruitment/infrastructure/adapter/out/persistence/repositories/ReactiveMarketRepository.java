package com.punks.recruitment.infrastructure.adapter.out.persistence.repositories;

import com.punks.recruitment.infrastructure.adapter.out.persistence.entities.MarketEntity;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

public interface ReactiveMarketRepository extends ReactiveCrudRepository<MarketEntity, UUID> {
	Mono<MarketEntity> findByMarketId(String marketId);

	Flux<MarketEntity> findAllByEnabledAndUpdatedAtAfter(Boolean enabled, Instant updatedAt);

	@Modifying
	@Query("""
			INSERT INTO markets(market_id, base_token, quote_token, price, enabled, updated_at)
			VALUES (:#{#marketEntity.marketId}, :#{#marketEntity.baseToken}, :#{#marketEntity.quoteToken}, :#{#marketEntity.price}, :#{#marketEntity.enabled}, :#{#marketEntity.updatedAt})
			ON CONFLICT (market_id)
			DO UPDATE SET price = EXCLUDED.price,
			              enabled = EXCLUDED.enabled,
			              updated_at = EXCLUDED.updated_at
			""")
	Mono<Integer> upsert(@Param("marketEntity") MarketEntity marketEntity);

}
