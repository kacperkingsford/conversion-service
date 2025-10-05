package com.punks.recruitment.infrastructure.adapter.in.kafka.message;

import com.punks.recruitment.domain.convert.ConvertCommand;
import com.punks.recruitment.domain.id.CommandId;
import com.punks.recruitment.domain.id.IdempotencyKey;
import com.punks.recruitment.domain.market.MarketId;
import com.punks.recruitment.domain.market.MarketPriceChanged;
import com.punks.recruitment.domain.market.MarketStatusChanged;
import com.punks.recruitment.domain.money.Amount;
import com.punks.recruitment.domain.money.Price;
import com.punks.recruitment.domain.route.Token;
import com.punks.recruitment.infrastructure.adapter.in.kafka.message.exception.InvalidMarketIdFormatException;

import java.util.Optional;

public class InboundMapper {

	private InboundMapper() {
	}

	public static ConvertCommand toDomain(ConvertTokenMessage tokenMessage) {
		return new ConvertCommand(
				new CommandId(tokenMessage.commandId()),
				Optional.ofNullable(tokenMessage.idempotencyKey()).filter(s -> !s.isBlank()).map(IdempotencyKey::new),
				new Token(tokenMessage.tokenFrom()),
				new Token(tokenMessage.tokenTo()),
				new Amount(tokenMessage.amount()),
				tokenMessage.requestedAt()
		);
	}

	public static MarketPriceChanged toDomainPrice(MarketEventMessage eventMessage) {
		var id = toMarketId(eventMessage.marketId());
		return new MarketPriceChanged(id, new Price(eventMessage.price()), eventMessage.updatedAt());
	}

	public static MarketStatusChanged toDomainStatus(MarketEventMessage eventMessage) {
		var id = toMarketId(eventMessage.marketId());
		return new MarketStatusChanged(id, new Price(eventMessage.price()), eventMessage.updatedAt(), eventMessage.reason());
	}


	private static MarketId toMarketId(String marketId) {
		String[] parts = marketId.split("-");
		if (parts.length != 2)
			throw new InvalidMarketIdFormatException(marketId);
		return new MarketId(new Token(parts[0]), new Token(parts[1]));
	}
}
