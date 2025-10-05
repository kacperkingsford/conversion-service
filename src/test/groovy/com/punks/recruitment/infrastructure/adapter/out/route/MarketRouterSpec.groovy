package com.punks.recruitment.infrastructure.adapter.out.route

import com.punks.recruitment.application.port.out.clock.ClockPort
import com.punks.recruitment.application.port.out.market.MarketRoutingPort
import com.punks.recruitment.application.port.out.market.MarketStateRepositoryPort
import com.punks.recruitment.application.port.out.precision.PrecisionPolicyPort
import com.punks.recruitment.domain.market.MarketId
import com.punks.recruitment.domain.market.MarketSnapshot
import com.punks.recruitment.domain.money.Price
import com.punks.recruitment.domain.money.RateQuote
import com.punks.recruitment.domain.route.Route
import com.punks.recruitment.domain.route.Token
import reactor.core.publisher.Flux
import reactor.test.StepVerifier
import spock.lang.Specification

import java.math.MathContext
import java.time.Duration
import java.time.Instant

class MarketRouterSpec extends Specification {

    MarketStateRepositoryPort repo = Mock()
    PrecisionPolicyPort precision = Mock()
    ClockPort clock = Mock()
    MarketRoutingPort router

    Token A = new Token("A")
    Token B = new Token("B")
    Token C = new Token("C")
    Token D = new Token("D")
    Token USD = new Token("USD")
    Token EUR = new Token("EUR")

    Instant now = Instant.parse("2025-01-01T12:00:00Z")
    Duration maxAge = Duration.ofSeconds(10)

    MathContext mathContext = new MathContext(18)

    def setup() {
        precision.mathContext() >> mathContext
        clock.now() >> now
        router = new MarketRouter(repo, precision, clock)
    }

    def "directRate returns direct quote when base -> quote market exists"() {
        given:
        def price = "1.2345"
        def markets = [buildMarketSnapshot(EUR, USD, price, now)]
        def expectedCutoff = now - maxAge

        when:
        def mono = router.directRate(EUR, USD, maxAge)

        then:
        1 * repo.findAllEnabledSince({ it == expectedCutoff }) >> Flux.fromIterable(markets)

        and:
        StepVerifier.create(mono)
                .assertNext { RateQuote rq ->
                    assert rq.rate().value() == new BigDecimal(price)
                    assert rq.quotedAt() == now
                }
                .verifyComplete()
    }

    def "directRate returns inverse quote when only quote -> base market exists"() {
        given:
        def price = "3.00"
        def markets = [buildMarketSnapshot(USD, EUR, price, now)]
        def expected = BigDecimal.ONE.divide(new BigDecimal(price), precision.mathContext())
        def expectedCutoff = now - maxAge

        when:
        def mono = router.directRate(EUR, USD, maxAge)

        then:
        1 * repo.findAllEnabledSince({ it == expectedCutoff }) >> Flux.fromIterable(markets)

        and:
        StepVerifier.create(mono)
                .assertNext { RateQuote rq ->
                    assert rq.rate().value() == expected
                    assert rq.quotedAt() == now
                }
                .verifyComplete()
    }

    def "findAnyRoute returns empty when from == to"() {
        expect:
        StepVerifier.create(router.findAnyRoute(A, A, 4, maxAge))
                .verifyComplete()
    }

    def "findAnyRoute finds a 3-hop path via BFS (A->B->C->D)"() {
        given:
        def markets = [
                buildMarketSnapshot(A, B, "2.0", now),
                buildMarketSnapshot(B, C, "3.0", now),
                buildMarketSnapshot(C, D, "5.0", now),
        ]
        def expectedCutoff = now - maxAge

        when:
        def mono = router.findAnyRoute(A, D, 4, maxAge)

        then:
        1 * repo.findAllEnabledSince({ it == expectedCutoff }) >> Flux.fromIterable(markets)

        and:
        StepVerifier.create(mono)
                .assertNext { Route route ->
                    assert route.hops().size() == 3
                    assert route.hops().get(0).from() == A
                    assert route.hops().get(0).to() == B
                    assert route.hops().get(1).from() == B
                    assert route.hops().get(1).to() == C
                    assert route.hops().get(2).from() == C
                    assert route.hops().get(2).to() == D
                }
                .verifyComplete()
    }

    def "findAnyRoute respects maxHops and returns empty when path is longer"() {
        given:
        def markets = [
                buildMarketSnapshot(A, B, "2.0", now),
                buildMarketSnapshot(B, C, "3.0", now),
                buildMarketSnapshot(C, D, "5.0", now),
        ]
        def expectedCutoff = now - maxAge

        when:
        def mono = router.findAnyRoute(A, D, 2, maxAge)

        then:
        1 * repo.findAllEnabledSince({ it == expectedCutoff }) >> Flux.fromIterable(markets)

        and:
        StepVerifier.create(mono)
                .verifyComplete()
    }

    def "findAnyRoute handles cycles without infinite loop"() {
        given:
        def markets = [
                buildMarketSnapshot(A, B, "2.0", now),
                buildMarketSnapshot(B, A, "0.5", now),
                buildMarketSnapshot(B, C, "3.0", now),
        ]
        def expectedCutoff = now - maxAge

        when:
        def mono = router.findAnyRoute(A, C, 4, maxAge)

        then:
        1 * repo.findAllEnabledSince({ it == expectedCutoff }) >> Flux.fromIterable(markets)

        and:
        StepVerifier.create(mono)
                .assertNext { Route route ->
                    assert route.hops().size() == 2
                    assert route.hops().get(0).from() == A
                    assert route.hops().get(0).to() == B
                    assert route.hops().get(1).from() == B
                    assert route.hops().get(1).to() == C
                }
                .verifyComplete()
    }

    def "cutoff is computed from clock and passed to repository"() {
        given:
        def expectedCutoff = now - maxAge

        when:
        def mono = router.directRate(A, B, maxAge)

        then:
        1 * repo.findAllEnabledSince({ Instant cut -> cut == expectedCutoff }) >> Flux.fromIterable([])
        StepVerifier.create(mono).verifyComplete()
    }

    def "findAnyRoute multiplies hop rates across mixed direct and inverse hops"() {
        given:
        def markets = [
                buildMarketSnapshot(A, B, "2.0", now),
                buildMarketSnapshot(B, C, "3.0", now),
                buildMarketSnapshot(D, C, "10.0", now)
        ]
        def expectedCutoff = now - maxAge
        def expectedProduct = new BigDecimal("0.6")

        when:
        def mono = router.findAnyRoute(A, D, 4, maxAge)

        then:
        1 * repo.findAllEnabledSince({ it == expectedCutoff }) >> Flux.fromIterable(markets)

        and:
        StepVerifier.create(mono)
                .assertNext { Route route ->
                    assert route.hops().size() == 3
                    def product = route.hops()
                            .collect { it.rate().value() }
                            .inject(BigDecimal.ONE) { acc, r -> acc.multiply(r, mathContext) }
                    assert product == expectedProduct
                }
                .verifyComplete()
    }


    private static MarketSnapshot buildMarketSnapshot(Token base, Token quote, String price, Instant updatedAt) {
        return new MarketSnapshot(new MarketId(base, quote), new Price(new BigDecimal(price)), true, updatedAt)
    }
}
