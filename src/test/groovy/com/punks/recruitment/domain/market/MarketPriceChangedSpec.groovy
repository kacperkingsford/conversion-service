package com.punks.recruitment.domain.market

import com.punks.recruitment.domain.money.Price
import com.punks.recruitment.domain.route.Token
import spock.lang.Specification

import java.time.Instant

class MarketPriceChangedSpec extends Specification {

    def "should create enabled MarketSnapshot from MarketPriceChanged"() {
        given:
        def marketId = new MarketId(new Token("BTC"), new Token("USD"))
        def price = new Price(BigDecimal.valueOf(42000))
        def updatedAt = Instant.now()

        and:
        def event = new MarketPriceChanged(marketId, price, updatedAt)

        when:
        def snapshot = event.toEnabledSnapshot()

        then:
        snapshot.marketId() == marketId
        snapshot.price() == price
        snapshot.enabled()
        snapshot.updatedAt() == updatedAt
    }

    def "should create disabled MarketSnapshot from MarketPriceChanged"() {
        given:
        def marketId = new MarketId(new Token("ETH"), new Token("USD"))
        def price = new Price(BigDecimal.valueOf(3000))
        def updatedAt = Instant.now()

        and:
        def event = new MarketPriceChanged(marketId, price, updatedAt)

        when:
        def snapshot = event.toDisabledSnapshot()

        then:
        !snapshot.enabled()
        snapshot.marketId() == marketId
        snapshot.price() == price
        snapshot.updatedAt() == updatedAt
    }

    def "should fail when any argument is null"() {
        when:
        new MarketPriceChanged(marketId, price, updatedAt)

        then:
        thrown(NullPointerException)

        where:
        marketId                                         | price                     | updatedAt
        null                                             | new Price(BigDecimal.ONE) | Instant.now()
        new MarketId(new Token("BTC"), new Token("USD")) | null                      | Instant.now()
        new MarketId(new Token("BTC"), new Token("USD")) | new Price(BigDecimal.ONE) | null
    }
}
