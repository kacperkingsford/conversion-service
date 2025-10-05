package com.punks.recruitment.infrastructure.adapter.out.persistence.mapper

import com.punks.recruitment.domain.convert.ConversionId
import com.punks.recruitment.domain.id.CommandId
import com.punks.recruitment.domain.money.Amount
import com.punks.recruitment.domain.money.Rate
import com.punks.recruitment.domain.route.Hop
import com.punks.recruitment.domain.route.Route
import com.punks.recruitment.domain.route.Token
import com.punks.recruitment.domain.route.TokenConvertedEvent
import com.punks.recruitment.infrastructure.adapter.out.persistence.entities.OutboxEventEntity
import io.r2dbc.postgresql.codec.Json
import spock.lang.Specification

import java.time.Instant

class OutboxEventMapperSpec extends Specification {

    def "toEntity maps TokenConvertedEvent to OutboxEventEntity with payload and metadata"() {
        given: "a sample domain event"
        def convId = new ConversionId()
        def cmdId = new CommandId()
        def from = new Token("BTC")
        def to = new Token("USDT")
        def amountIn = new Amount(new BigDecimal("0.5"))
        def amountOut = new Amount(new BigDecimal("15000"))
        def hopTime = Instant.parse("2024-01-01T00:00:00Z")
        def hop = new Hop(from, to, new Rate(new BigDecimal("30000")), hopTime)
        def route = new Route(List.of(hop))
        def event = new TokenConvertedEvent(convId, cmdId, from, to, amountIn, amountOut, route, hopTime)

        and: "payload and timestamp for outbox"
        def payload = '{"some":"json"}'
        def createdAt = Instant.parse("2024-01-01T00:00:05Z")

        when:
        OutboxEventEntity e = OutboxEventMapper.toEntity(event, payload, createdAt)

        then: "metadata is mapped"
        e.getAggregateType() == "Conversion"
        e.getAggregateId() == convId.value().toString()
        e.getEventType() == "TokenConvertedEvent"

        and: "payload is wrapped as Json"
        e.getPayloadJson() instanceof Json
        e.getPayloadJson().asString() == payload

        and: "timestamps and flags are set"
        e.getCreatedAt() == createdAt
        e.getPublished() == Boolean.FALSE
        e.getPublishedAt() == null

        and: "ID is not set by mapper (DB will generate)"
        e.getEventId() == null
    }

    def "toEntity accepts empty payload and different ids"() {
        given:
        def convId = new ConversionId()
        def cmdId = new CommandId()
        def from = new Token("ETH")
        def to = new Token("EUR")
        def amountIn = new Amount(new BigDecimal("1"))
        def amountOut = new Amount(new BigDecimal("2500"))
        def t = Instant.parse("2025-02-02T12:00:00Z")
        def route = new Route(List.of(new Hop(from, to, new Rate(new BigDecimal("2500")), t)))
        def event = new TokenConvertedEvent(convId, cmdId, from, to, amountIn, amountOut, route, t)

        when:
        def e = OutboxEventMapper.toEntity(event, "", t)

        then:
        e.getAggregateId() == convId.value().toString()
        e.getPayloadJson() instanceof Json
        e.getPayloadJson().asString() == ""
        e.getCreatedAt() == t
        e.getPublished() == Boolean.FALSE
    }
}
