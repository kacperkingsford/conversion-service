package com.punks.recruitment.application.service.convert

import com.punks.recruitment.application.port.out.clock.ClockPort
import com.punks.recruitment.application.port.out.convert.ConversionRepository
import com.punks.recruitment.application.port.out.outbox.OutboxPort
import com.punks.recruitment.application.port.out.precision.PrecisionPolicyPort
import com.punks.recruitment.application.service.convert.mapper.ConversionMapper
import com.punks.recruitment.application.service.route.RouteFinder
import com.punks.recruitment.domain.convert.ConversionRecord
import com.punks.recruitment.domain.convert.ConvertCommand
import com.punks.recruitment.domain.id.CommandId
import com.punks.recruitment.domain.id.IdempotencyKey
import com.punks.recruitment.domain.money.Amount
import com.punks.recruitment.domain.money.Rate
import com.punks.recruitment.domain.route.*
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import spock.lang.Ignore
import spock.lang.Specification
import spock.lang.Subject

import java.math.MathContext
import java.time.Instant

@Ignore // TODO
class ConvertTokenServiceSpec extends Specification {

    def conversions = Mock(ConversionRepository)
    def outbox = Mock(OutboxPort)
    def clock = Mock(ClockPort)
    def precision = Mock(PrecisionPolicyPort)
    def routeFinder = Mock(RouteFinder)

    @Subject
    def service = new ConvertTokenService(conversions, outbox, clock, precision, routeFinder)

    private static ConversionRecord recordSucceeded(ConvertCommand cmd, Route route, Amount amount, Instant now) {
        return ConversionRecord.succeeded(cmd, amount, route, now)
    }

    private static ConversionRecord recordFailed(ConvertCommand cmd, Instant now) {
        return ConversionRecord.failedNoRoute(cmd, now)
    }

    def "should return existing conversion if already stored (idempotent)"() {
        given:
        def now = Instant.parse("2024-01-01T00:00:00Z")
        def cmd = new ConvertCommand(
                new CommandId(),
                Optional.of(new IdempotencyKey("idem-key")),
                new Token("BTC"),
                new Token("ETH"),
                new Amount(BigDecimal.ONE),
                now
        )
        def existing = recordSucceeded(cmd, new Route([]), new Amount(BigDecimal.ONE), now)

        conversions.findByCommand(cmd.commandId()) >> Mono.just(Optional.of(existing))
        conversions.findByIdempotency((new IdempotencyKey("idem-key"))) >> Mono.just(Optional.empty())

        when:
        def resultMono = service.convert(cmd)

        then:
        StepVerifier.create(resultMono)
                .expectNext(ConversionMapper.toResult(existing))
                .verifyComplete()

        and:
        0 * routeFinder.findRoute(_, _)
        0 * conversions.saveIfAbsent(_)
    }

    def "should compute new conversion if not found"() {
        given:
        def from = new Token("BTC")
        def to = new Token("ETH")
        def now = Instant.parse("2024-01-01T00:00:00Z")
        def cmd = new ConvertCommand(new CommandId(), Optional.empty(), from, to, new Amount(BigDecimal.TEN), now)
        def route = new Route([new Hop(from, to, new Rate(BigDecimal.valueOf(2)), now)])
        def convertedAmount = new Amount(BigDecimal.valueOf(20))
        def record = recordSucceeded(cmd, route, convertedAmount, now)

        conversions.findByCommand(_) >> Mono.just(Optional.empty())
        conversions.findByIdempotency(_) >> Mono.just(Optional.empty())
        routeFinder.findRoute(from, to) >> Mono.just(route)
        precision.scaleFor(_) >> 2
        precision.mathContext() >> new MathContext(10)
        clock.now() >> now
        conversions.saveIfAbsent(_) >> Mono.just(record)
        outbox.append(_ as TokenConvertedEvent) >> Mono.empty()

        when:
        def resultMono = service.convert(cmd)

        then:
        StepVerifier.create(resultMono)
                .expectNext(ConversionMapper.toResult(record))
                .verifyComplete()

        and:
        1 * conversions.saveIfAbsent(_)
        1 * outbox.append(_ as TokenConvertedEvent)
    }

    def "should save failed record when no route found"() {
        given:
        def from = new Token("BTC")
        def to = new Token("DOGE")
        def now = Instant.parse("2024-01-01T00:00:00Z")
        def cmd = new ConvertCommand(new CommandId(), Optional.empty(), from, to, new Amount(BigDecimal.ONE), now)
        def failed = recordFailed(cmd, now)

        conversions.findByCommand(_) >> Mono.just(Optional.empty())
        conversions.findByIdempotency(_) >> Mono.just(Optional.empty())
        routeFinder.findRoute(_, _) >> Mono.empty()
        clock.now() >> now
        conversions.saveIfAbsent(_ as ConversionRecord) >> Mono.just(failed)

        when:
        def resultMono = service.convert(cmd)

        then:
        StepVerifier.create(resultMono)
                .expectNext(ConversionMapper.toResult(failed))
                .verifyComplete()

        and:
        1 * conversions.saveIfAbsent(_)
        0 * outbox.append(_)
    }

    def "should not publish outbox event when record already exists"() {
        given:
        def from = new Token("BTC")
        def to = new Token("ETH")
        def now = Instant.parse("2024-01-01T00:00:00Z")
        def cmd = new ConvertCommand(new CommandId(), Optional.empty(), from, to, new Amount(BigDecimal.ONE), now)
        def route = new Route([new Hop(from, to, new Rate(BigDecimal.ONE), now)])
        def record = recordSucceeded(cmd, route, new Amount(BigDecimal.ONE), now)
        def existing = recordSucceeded(cmd, route, new Amount(BigDecimal.ONE), now)

        conversions.findByCommand(_) >> Mono.just(Optional.empty())
        conversions.findByIdempotency(_) >> Mono.just(Optional.empty())
        routeFinder.findRoute(_, _) >> Mono.just(route)
        precision.scaleFor(_) >> 2
        precision.mathContext() >> new MathContext(10)
        clock.now() >> now
        conversions.saveIfAbsent(_) >> Mono.just(existing)

        when:
        def resultMono = service.convert(cmd)

        then:
        StepVerifier.create(resultMono)
                .expectNext(ConversionMapper.toResult(existing))
                .verifyComplete()

        and:
        0 * outbox.append(_)
    }
}
