package com.punks.recruitment.infrastructure.adapter.out.persistence.mapper

import com.punks.recruitment.domain.market.MarketId
import com.punks.recruitment.domain.market.MarketSnapshot
import com.punks.recruitment.domain.money.Price
import com.punks.recruitment.domain.route.Token
import com.punks.recruitment.infrastructure.adapter.out.persistence.entities.MarketEntity
import spock.lang.Specification

import java.time.Instant

class MarketEntityMapperSpec extends Specification {

    def "toSnapshot maps MarketEntity → MarketSnapshot"() {
        given:
        def entity = new MarketEntity()
        entity.setMarketId("BTC-USDT")
        entity.setBaseToken("BTC")
        entity.setQuoteToken("USDT")
        entity.setPrice(new BigDecimal("30000.00"))
        entity.setEnabled(true)
        entity.setUpdatedAt(Instant.parse("2025-01-01T10:00:00Z"))

        when:
        def snap = MarketEntityMapper.toSnapshot(entity)

        then:
        snap.marketId() == new MarketId(new Token("BTC"), new Token("USDT"))
        snap.price().value() == new BigDecimal("30000.00")
        snap.enabled()
        snap.updatedAt() == Instant.parse("2025-01-01T10:00:00Z")
    }

    def "toEntity maps MarketSnapshot → MarketEntity"() {
        given:
        def id = new MarketId(new Token("ETH"), new Token("USDT"))
        def snap = new MarketSnapshot(
                id,
                new Price(new BigDecimal("2100.55")),
                false,
                Instant.parse("2025-01-01T10:01:00Z")
        )

        when:
        def entity = MarketEntityMapper.toEntity(snap)

        then:
        entity.getBaseToken() == "ETH"
        entity.getQuoteToken() == "USDT"
        entity.getPrice() == new BigDecimal("2100.55")
        !entity.getEnabled()
        entity.getUpdatedAt() == Instant.parse("2025-01-01T10:01:00Z")
        entity.getMarketId() == id.toString()
    }

    def "round-trip entity → snapshot → entity preserves tokens/price/enabled/updatedAt"() {
        given:
        def source = new MarketEntity()
        source.setMarketId("ignored-format")
        source.setBaseToken("SOL")
        source.setQuoteToken("USDT")
        source.setPrice(new BigDecimal("100.000"))
        source.setEnabled(true)
        source.setUpdatedAt(Instant.parse("2024-06-01T00:00:00Z"))

        when:
        def snap = MarketEntityMapper.toSnapshot(source)
        def target = MarketEntityMapper.toEntity(snap)

        then:
        snap.marketId().base().value() == "SOL"
        snap.marketId().quote().value() == "USDT"
        snap.price().value() == new BigDecimal("100.000")
        snap.enabled()
        snap.updatedAt() == Instant.parse("2024-06-01T00:00:00Z")

        and: "back to entity"
        target.getBaseToken() == "SOL"
        target.getQuoteToken() == "USDT"
        target.getPrice() == new BigDecimal("100.000")
        target.getEnabled()
        target.getUpdatedAt() == Instant.parse("2024-06-01T00:00:00Z")
        target.getMarketId() == snap.marketId().toString()
    }

    def "toSnapshot derives MarketId from base/quote even if entity.marketId differs"() {
        given:
        def entity = new MarketEntity()
        entity.setMarketId("WRONG_FORMAT_SHOULD_BE_IGNORED")
        entity.setBaseToken("ADA")
        entity.setQuoteToken("EUR")
        entity.setPrice(new BigDecimal("1.23"))
        entity.setEnabled(false)
        entity.setUpdatedAt(Instant.parse("2025-02-01T12:00:00Z"))

        when:
        def snap = MarketEntityMapper.toSnapshot(entity)

        then:
        snap.marketId() == new MarketId(new Token("ADA"), new Token("EUR"))
        snap.price().value() == new BigDecimal("1.23")
        !snap.enabled()
        snap.updatedAt() == Instant.parse("2025-02-01T12:00:00Z")
    }
}
