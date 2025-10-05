package com.punks.recruitment.domain.market

import com.punks.recruitment.domain.money.Price
import com.punks.recruitment.domain.route.Token
import spock.lang.Specification

import java.time.Instant

class MarketStatusChangedSpec extends Specification {

    def "should create enabled MarketSnapshot from MarketStatusChanged"() {
        given:
        def marketId = new MarketId(new Token("BTC"), new Token("EUR"))
        def price = new Price(BigDecimal.valueOf(41000))
        def updatedAt = Instant.now()
        def reason = "Market reopened"

        and:
        def event = new MarketStatusChanged(marketId, price, updatedAt, reason)

        when:
        def snapshot = event.toEnabledSnapshot()

        then:
        snapshot.marketId() == marketId
        snapshot.price() == price
        snapshot.enabled()
        snapshot.updatedAt() == updatedAt
    }

    def "should create disabled MarketSnapshot from MarketStatusChanged"() {
        given:
        def marketId = new MarketId(new Token("DOGE"), new Token("USD"))
        def price = new Price(BigDecimal.valueOf(0.12))
        def updatedAt = Instant.now()
        def reason = "Maintenance"

        and:
        def event = new MarketStatusChanged(marketId, price, updatedAt, reason)

        when:
        def snapshot = event.toDisabledSnapshot()

        then:
        !snapshot.enabled()
        snapshot.marketId() == marketId
        snapshot.price() == price
        snapshot.updatedAt() == updatedAt
    }

    def "should fail when any required argument is null"() {
        when:
        new MarketStatusChanged(marketId, price, updatedAt, "reason")

        then:
        thrown(NullPointerException)

        where:
        marketId                                         | price                     | updatedAt
        null                                             | new Price(BigDecimal.ONE) | Instant.now()
        new MarketId(new Token("BTC"), new Token("USD")) | null                      | Instant.now()
        new MarketId(new Token("BTC"), new Token("USD")) | new Price(BigDecimal.ONE) | null
    }
}
