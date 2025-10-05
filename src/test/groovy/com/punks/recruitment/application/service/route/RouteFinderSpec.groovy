package com.punks.recruitment.application.service.route

import com.punks.recruitment.application.port.out.market.MarketRoutingPort
import com.punks.recruitment.application.service.convert.RouteQueryFactory
import com.punks.recruitment.application.service.convert.dto.RouteQuery
import com.punks.recruitment.domain.money.Rate
import com.punks.recruitment.domain.money.RateQuote
import com.punks.recruitment.domain.route.*
import reactor.core.publisher.Mono
import spock.lang.Specification
import spock.lang.Subject

import java.time.Duration
import java.time.Instant

class RouteFinderSpec extends Specification {

    def router = Mock(MarketRoutingPort)
    def routeQueryFactory = Mock(RouteQueryFactory)

    @Subject
    def routeFinder = new RouteFinder(router, routeQueryFactory)

    def "should return direct route when direct rate is available"() {
        given:
        def from = new Token("BTC")
        def to = new Token("ETH")
        def rate = new Rate(new BigDecimal("0.05"))
        def rateQuote = new RateQuote(rate, Instant.parse("2024-01-01T00:00:00Z"))
        def query = new RouteQuery(3, Duration.ofSeconds(30))

        routeQueryFactory.defaultQuery() >> query
        router.directRate(from, to, query.maxPriceAge()) >> Mono.just(rateQuote)

        when:
        def result = routeFinder.findRoute(from, to).block()

        then:
        result != null
        result.hops.size() == 1

        with(result.hops.first()) {
            from == new Token("BTC")
            to == new Token("ETH")
            rate == new Rate(new BigDecimal("0.05"))
            quotedAt == Instant.parse("2024-01-01T00:00:00Z")
        }

        and:
        0 * router.findAnyRoute(_, _, _, _)
    }

    def "should return empty when both direct and any route are empty"() {
        given:
        def from = new Token("EUR")
        def to = new Token("PLN")
        def query = new RouteQuery(2, Duration.ofSeconds(10))

        routeQueryFactory.defaultQuery() >> query
        router.directRate(from, to, query.maxPriceAge()) >> Mono.empty()
        router.findAnyRoute(from, to, query.maxHops(), query.maxPriceAge()) >> Mono.empty()

        when:
        def result = routeFinder.findRoute(from, to).blockOptional()

        then:
        !result.isPresent()
    }
}
