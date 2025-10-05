package com.punks.recruitment.domain.route

import com.punks.recruitment.domain.money.Amount
import com.punks.recruitment.domain.money.PrecisionPolicy
import com.punks.recruitment.domain.money.Rate
import spock.lang.Specification

import java.time.Instant
import java.math.MathContext
import java.math.RoundingMode

class RouteSpec extends Specification {

    def precision = Mock(PrecisionPolicy)

    def "should multiply rates along hops and apply precision scale"() {
        given:
        def from = new Token("BTC")
        def mid = new Token("ETH")
        def to = new Token("USDT")

        def hop1 = new Hop(from, mid, new Rate(new BigDecimal("2.0")), Instant.now())
        def hop2 = new Hop(mid, to, new Rate(new BigDecimal("3.5")), Instant.now())

        def route = new Route([hop1, hop2])
        def amountIn = new Amount(new BigDecimal("10.00"))

        precision.mathContext() >> new MathContext(10)
        precision.scaleFor(to) >> 4

        when:
        def result = route.convert(amountIn, precision)

        then:
        // 10 * 2.0 * 3.5 = 70.0000
        result.value() == new BigDecimal("70.0000")
    }

    def "should handle single-hop route correctly"() {
        given:
        def from = new Token("EUR")
        def to = new Token("USD")
        def hop = new Hop(from, to, new Rate(new BigDecimal("1.12345")), Instant.now())
        def route = new Route([hop])
        def amountIn = new Amount(new BigDecimal("100.00"))

        precision.mathContext() >> new MathContext(10)
        precision.scaleFor(to) >> 2

        when:
        def result = route.convert(amountIn, precision)

        then:
        // 100 * 1.12345 = 112.345 -> 112.35 (precision)
        result.value() == new BigDecimal("112.35")
    }

    def "should round HALF_UP according to scale"() {
        given:
        def from = new Token("A")
        def to = new Token("B")
        def hop = new Hop(from, to, new Rate(new BigDecimal("1.005")), Instant.now())
        def route = new Route([hop])
        def amountIn = new Amount(new BigDecimal("100.00"))

        precision.mathContext() >> new MathContext(10)
        precision.scaleFor(to) >> 2

        when:
        def result = route.convert(amountIn, precision)

        then:
        result.value() == new BigDecimal("100.50")
    }

    def "isEmpty should return true for null or empty hops"() {
        expect:
        new Route(null).isEmpty()
        new Route([]).isEmpty()
        !new Route([new Hop(new Token("A"), new Token("B"), new Rate(BigDecimal.ONE), Instant.now())]).isEmpty()
    }

    def "should use MathContext precision when multiplying"() {
        given:
        def from = new Token("BTC")
        def to = new Token("USDT")
        def hop = new Hop(from, to, new Rate(new BigDecimal("1.123456789")), Instant.now())
        def route = new Route([hop])
        def amountIn = new Amount(new BigDecimal("1.23456789"))

        precision.mathContext() >> new MathContext(5, RoundingMode.HALF_UP)
        precision.scaleFor(to) >> 4

        when:
        def result = route.convert(amountIn, precision)

        then:
        result.value().scale() == 4
        result.value() == new BigDecimal("1.3870")
    }
}
