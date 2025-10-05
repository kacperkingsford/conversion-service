package com.punks.recruitment.infrastructure.adapter.in.kafka.message

import com.punks.recruitment.domain.convert.ConvertCommand
import com.punks.recruitment.domain.id.CommandId
import com.punks.recruitment.domain.id.IdempotencyKey
import com.punks.recruitment.domain.market.MarketId
import com.punks.recruitment.domain.market.MarketPriceChanged
import com.punks.recruitment.domain.market.MarketStatusChanged
import com.punks.recruitment.domain.money.Amount
import com.punks.recruitment.domain.money.Price
import com.punks.recruitment.domain.route.Token
import com.punks.recruitment.infrastructure.adapter.in.kafka.message.exception.InvalidMarketIdFormatException
import spock.lang.Specification

import java.time.Instant

class InboundMapperSpec extends Specification {

    def "should map ConvertTokenMessage to ConvertCommand with idempotency key"() {
        given:
        def uuid = UUID.randomUUID()
        def message = new ConvertTokenMessage(
                uuid,
                "idem-123",
                "BTC",
                "USDT",
                new BigDecimal("100.50"),
                Instant.parse("2025-01-01T00:00:00Z")
        )

        when:
        ConvertCommand cmd = InboundMapper.toDomain(message)

        then:
        cmd.commandId() == new CommandId(uuid)
        cmd.idempotencyKey().get() == new IdempotencyKey("idem-123")
        cmd.tokenFrom() == new Token("BTC")
        cmd.tokenTo() == new Token("USDT")
        cmd.amount() == new Amount(new BigDecimal("100.50"))
        cmd.requestedAt() == Instant.parse("2025-01-01T00:00:00Z")
    }

    def "should map ConvertTokenMessage to ConvertCommand without idempotency key when blank or null"() {
        given:
        def uuid = UUID.randomUUID()
        def message = new ConvertTokenMessage(
                uuid,
                idempotencyKey,
                "ETH",
                "EUR",
                new BigDecimal("50.00"),
                Instant.parse("2025-02-02T00:00:00Z")
        )

        when:
        ConvertCommand cmd = InboundMapper.toDomain(message)

        then:
        !cmd.idempotencyKey().isPresent()

        where:
        idempotencyKey << [null, "", "   "]
    }

    def "should map MarketEventMessage to MarketPriceChanged"() {
        given:
        def message = new MarketEventMessage(
                "MarketPriceChanged",
                "BTC-USDT",
                new BigDecimal("30000.00"),
                Instant.parse("2025-01-01T10:00:00Z"),
                null
        )

        when:
        MarketPriceChanged event = InboundMapper.toDomainPrice(message)

        then:
        event.marketId() == new MarketId(new Token("BTC"), new Token("USDT"))
        event.price() == new Price(new BigDecimal("30000.00"))
        event.updatedAt() == Instant.parse("2025-01-01T10:00:00Z")
    }

    def "should map MarketEventMessage to MarketStatusChanged"() {

        given:
        def message = new MarketEventMessage(
                "MarketEnabled",
                "ETH-USD",
                new BigDecimal("2100.55"),
                Instant.parse("2025-01-01T10:01:00Z"),
                "Enabling instrument pair..."
        )
        when:
        MarketStatusChanged event = InboundMapper.toDomainStatus(message)

        then:
        event.marketId() == new MarketId(new Token("ETH"), new Token("USD"))
        event.price() == new Price(new BigDecimal("2100.55"))
        event.updatedAt() == Instant.parse("2025-01-01T10:01:00Z")
        event.reason() == "Enabling instrument pair..."
    }

    def "should throw InvalidMarketIdFormatException for malformed marketId"() {
        when:
        InboundMapper.toDomainPrice(new MarketEventMessage(
                "MarketPriceChanged",
                badMarketId,
                new BigDecimal("123.45"),
                Instant.now(),
                null
        ))

        then:
        def ex = thrown(InvalidMarketIdFormatException)
        ex.message.contains(badMarketId)

        where:
        badMarketId << ["BTCUSDT", "BTC-USDT-EUR", "BTC"]
    }
}
