package com.punks.recruitment.application.service.market

import com.punks.recruitment.application.port.out.market.MarketCachePort
import com.punks.recruitment.application.port.out.market.MarketStateRepositoryPort
import com.punks.recruitment.domain.market.*
import com.punks.recruitment.domain.money.Price
import com.punks.recruitment.domain.route.Token
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import spock.lang.Specification

import java.time.Instant

class MarketUpdateServiceSpec extends Specification {

    def markets = Mock(MarketStateRepositoryPort)
    def cache = Mock(MarketCachePort)
    def service = new MarketUpdateService(markets, cache)

    def "onPriceChanged should upsert snapshot and push price to cache"() {
        given:
        def marketId = new MarketId(new Token("BTC"), new Token("USDT"))
        def price = new Price(new BigDecimal("30000"))
        def timestamp = Instant.parse("2025-01-01T00:00:00Z")
        def event = new MarketPriceChanged(marketId, price, timestamp)

        when:
        StepVerifier.create(service.onPriceChanged(event))
                .verifyComplete()

        then:
        1 * markets.upsert({ MarketSnapshot s ->
            s.marketId() == marketId &&
                    s.enabled() &&
                    s.price().value() == price.value() &&
                    s.updatedAt() == timestamp
        }) >> Mono.empty()

        1 * cache.applyPrice({ MarketSnapshot s ->
            s.marketId() == marketId &&
                    s.enabled() &&
                    s.price().value() == price.value() &&
                    s.updatedAt() == timestamp
        }) >> Mono.empty()

        0 * _
    }

    def "onMarketEnabled should upsert with event price and enable when market exists"() {
        given:
        def marketId = new MarketId(new Token("ETH"), new Token("USDT"))
        def existing = new MarketSnapshot(marketId, new Price(new BigDecimal("2000.00")), false,
                Instant.parse("2025-01-01T10:00:00Z"))
        def eventPrice = new Price(new BigDecimal("2100.55"))
        def eventTimestamp = Instant.parse("2025-01-01T10:01:00Z")
        def event = new MarketStatusChanged(marketId, eventPrice, eventTimestamp, "Enabling")

        when:
        StepVerifier.create(service.onMarketEnabled(event)).verifyComplete()

        then:
        1 * markets.findById(marketId) >> { Mono.defer { Mono.just(existing) } }

        1 * markets.upsert({ MarketSnapshot s ->
            s.marketId() == marketId &&
                    s.enabled() &&
                    s.price().value() == eventPrice.value() &&
                    s.updatedAt() == eventTimestamp
        }) >> Mono.empty()

        1 * cache.applyStatus({ MarketSnapshot s ->
            s.marketId() == marketId &&
                    s.enabled() &&
                    s.price().value() == eventPrice.value() &&
                    s.updatedAt() == eventTimestamp
        }) >> Mono.empty()

        0 * _
    }

    def "onMarketEnabled should be NOOP when market not found"() {
        given:
        def marketId = new MarketId(new Token("DOGE"), new Token("EUR"))
        def eventPrice = new Price(new BigDecimal("0.10"))
        def eventTimestamp = Instant.parse("2025-02-02T12:00:00Z")
        def event = new MarketStatusChanged(marketId, eventPrice, eventTimestamp, "updating...")

        when:
        StepVerifier.create(service.onMarketEnabled(event)).verifyComplete()

        then:
        1 * markets.findById(marketId) >> Mono.empty()
        0 * markets.upsert(_)
        0 * cache.applyStatus(_)
    }

    def "onMarketDisabled should upsert with event price and disable when market exists"() {
        given:
        def marketId = new MarketId(new Token("SOL"), new Token("USDT"))
        def existing = new MarketSnapshot(marketId, new Price(new BigDecimal("100.00")), true,
                Instant.parse("2025-03-01T00:00:00Z"))
        def eventPrice = new Price(new BigDecimal("90.50"))
        def eventTimestamp = Instant.parse("2025-03-01T00:05:00Z")
        def event = new MarketStatusChanged(marketId, eventPrice, eventTimestamp, "Disabling...")

        when:
        StepVerifier.create(service.onMarketDisabled(event)).verifyComplete()

        then:
        1 * markets.findById(marketId) >> { Mono.defer { Mono.just(existing) } }

        1 * markets.upsert({ MarketSnapshot s ->
            s.marketId() == marketId &&
                    !s.enabled() &&
                    s.price().value() == eventPrice.value() &&
                    s.updatedAt() == eventTimestamp
        }) >> Mono.empty()

        1 * cache.applyStatus({ MarketSnapshot s ->
            s.marketId() == marketId &&
                    !s.enabled() &&
                    s.price().value() == eventPrice.value() &&
                    s.updatedAt() == eventTimestamp
        }) >> Mono.empty()

        0 * _
    }

    def "onMarketDisabled should be NOOP when market not found"() {
        given:
        def marketId = new MarketId(new Token("X"), new Token("Y"))
        def eventPrice = new Price(new BigDecimal("1.23"))
        def eventTimestamp = Instant.parse("2025-04-01T00:00:00Z")
        def event = new MarketStatusChanged(marketId, eventPrice, eventTimestamp, "sync")

        when:
        StepVerifier.create(service.onMarketDisabled(event)).verifyComplete()

        then:
        1 * markets.findById(marketId) >> Mono.empty()
        0 * markets.upsert(_)
        0 * cache.applyStatus(_)
    }
}
