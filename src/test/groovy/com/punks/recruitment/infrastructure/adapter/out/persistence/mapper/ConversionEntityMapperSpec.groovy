package com.punks.recruitment.infrastructure.adapter.out.persistence.mapper

import com.punks.recruitment.domain.convert.ConversionId
import com.punks.recruitment.domain.convert.ConversionRecord
import com.punks.recruitment.domain.convert.ConversionStatus
import com.punks.recruitment.domain.id.CommandId
import com.punks.recruitment.domain.id.IdempotencyKey
import com.punks.recruitment.domain.money.Amount
import com.punks.recruitment.domain.money.Rate
import com.punks.recruitment.domain.route.Hop
import com.punks.recruitment.domain.route.Route
import com.punks.recruitment.domain.route.Token
import com.punks.recruitment.infrastructure.adapter.out.persistence.entities.ConversionEntity
import io.r2dbc.postgresql.codec.Json
import spock.lang.Specification

import java.time.Instant

class ConversionEntityMapperSpec extends Specification {

    def "toEntity maps all fields incl. idempotency and JSON path"() {
        given:
        def convId = new ConversionId()
        def cmdId  = new CommandId()
        def key    = new IdempotencyKey("idem-123")
        def from   = new Token("BTC")
        def to     = new Token("USDT")
        def inAmt  = new Amount(new BigDecimal("0.5"))
        def outAmt = new Amount(new BigDecimal("15000"))
        def quoted = Instant.parse("2024-01-01T00:00:05Z")
        def route  = new Route(List.of(
                new Hop(from, to, new Rate(new BigDecimal("30000")), Instant.parse("2024-01-01T00:00:00Z"))
        ))
        def record = new ConversionRecord(convId, cmdId, Optional.of(key), from, to, inAmt, outAmt, route, quoted, ConversionStatus.SUCCEEDED)

        and: "JSON produced elsewhere (here hardcoded)"
        def routeJson = '{"hops":[{"from":{"value":"BTC"},"to":{"value":"USDT"},"rate":{"value":30000},"quotedAt":"2024-01-01T00:00:00Z"}]}'

        when:
        def entity = ConversionEntityMapper.toEntity(record, routeJson)

        then:
        entity.getCommandId()       == cmdId.value()
        entity.getIdempotencyKey()  == "idem-123"
        entity.getTokenFrom()       == "BTC"
        entity.getTokenTo()         == "USDT"
        entity.getAmountIn()        == new BigDecimal("0.5")
        entity.getAmountOut()       == new BigDecimal("15000")
        entity.getComputedAt()      == quoted
        entity.getStatus()          == "SUCCEEDED"
        entity.getPathJson()        instanceof Json
        entity.getPathJson().asString() == routeJson
    }

    def "toEntity sets idempotencyKey to null when Optional.empty()"() {
        given:
        def record = new ConversionRecord(
                new ConversionId(),
                new CommandId(),
                Optional.empty(),
                new Token("ETH"),
                new Token("USDT"),
                new Amount(new BigDecimal("1.0")),
                new Amount(new BigDecimal("2100")),
                new Route(List.of()),
                Instant.parse("2025-01-01T00:00:00Z"),
                ConversionStatus.SUCCEEDED
        )

        when:
        def entity = ConversionEntityMapper.toEntity(record, "{}")

        then:
        entity.getIdempotencyKey() == null
    }

    def "toDomain maps entity to domain with provided Route and present idempotency"() {
        given:
        def e = new ConversionEntity()
        def convUuid = UUID.randomUUID()
        def cmdUuid  = UUID.randomUUID()
        e.setConversionId(convUuid)
        e.setCommandId(cmdUuid)
        e.setIdempotencyKey("idem-xyz")
        e.setTokenFrom("SOL")
        e.setTokenTo("USDT")
        e.setAmountIn(new BigDecimal("10"))
        e.setAmountOut(new BigDecimal("1000"))
        e.setComputedAt(Instant.parse("2024-06-01T00:00:01Z"))
        e.setStatus("SUCCEEDED")
        e.setPathJson(Json.of('{"hops":[]}'))

        and:
        def route = new Route(List.of(
                new Hop(new Token("SOL"), new Token("USDT"), new Rate(new BigDecimal("100")), Instant.parse("2024-06-01T00:00:00Z"))
        ))

        when:
        def rec = ConversionEntityMapper.toDomain(e, route)

        then:
        rec.conversionId()   == new ConversionId(convUuid)
        rec.commandId()      == new CommandId(cmdUuid)
        rec.idempotencyKey().isPresent()
        rec.idempotencyKey().get() == new IdempotencyKey("idem-xyz")
        rec.tokenFrom()      == new Token("SOL")
        rec.tokenTo()        == new Token("USDT")
        rec.amountIn().value()  == new BigDecimal("10")
        rec.amountOut().value() == new BigDecimal("1000")
        rec.computedAt()     == Instant.parse("2024-06-01T00:00:01Z")
        rec.status()         == ConversionStatus.SUCCEEDED
        rec.route().hops().size() == 1
        with(rec.route().hops().get(0)) {
            from() == new Token("SOL")
            to()   == new Token("USDT")
            rate().value() == new BigDecimal("100")
        }
    }

    def "toDomain maps entity to domain with empty idempotency when null"() {
        given:
        def e = new ConversionEntity()
        e.setConversionId(UUID.randomUUID())
        e.setCommandId(UUID.randomUUID())
        e.setIdempotencyKey(null)
        e.setTokenFrom("X")
        e.setTokenTo("Y")
        e.setAmountIn(new BigDecimal("1"))
        e.setAmountOut(new BigDecimal("2"))
        e.setComputedAt(Instant.parse("2025-01-01T00:00:00Z"))
        e.setStatus("FAILED")
        e.setPathJson(Json.of('{"hops":[]}'))

        and:
        def route = new Route(List.of())

        when:
        def rec = ConversionEntityMapper.toDomain(e, route)

        then:
        !rec.idempotencyKey().isPresent()
        rec.tokenFrom() == new Token("X")
        rec.tokenTo()   == new Token("Y")
        rec.status()    == ConversionStatus.FAILED
    }
}
