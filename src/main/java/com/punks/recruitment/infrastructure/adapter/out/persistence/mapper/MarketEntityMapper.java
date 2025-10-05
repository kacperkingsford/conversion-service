package com.punks.recruitment.infrastructure.adapter.out.persistence.mapper;

import com.punks.recruitment.domain.market.MarketId;
import com.punks.recruitment.domain.market.MarketSnapshot;
import com.punks.recruitment.domain.money.Price;
import com.punks.recruitment.domain.route.Token;
import com.punks.recruitment.infrastructure.adapter.out.persistence.entities.MarketEntity;

public class MarketEntityMapper {

	private MarketEntityMapper() {
	}

	public static MarketSnapshot toSnapshot(MarketEntity marketEntity) {
		MarketId id = new MarketId(new Token(marketEntity.getBaseToken()), new Token(marketEntity.getQuoteToken()));
		return new MarketSnapshot(id, new Price(marketEntity.getPrice()), marketEntity.getEnabled(), marketEntity.getUpdatedAt());
	}

	public static MarketEntity toEntity(MarketSnapshot marketSnapshot) {
		String base = marketSnapshot.marketId().base().value();
		String quote = marketSnapshot.marketId().quote().value();

		MarketEntity marketEntity = new MarketEntity();
		marketEntity.setBaseToken(base);
		marketEntity.setQuoteToken(quote);
		marketEntity.setMarketId(marketSnapshot.marketId().toString());
		marketEntity.setPrice(marketSnapshot.price().value());
		marketEntity.setEnabled(marketSnapshot.enabled());
		marketEntity.setUpdatedAt(marketSnapshot.updatedAt());
		return marketEntity;
	}
}
