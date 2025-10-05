package com.punks.recruitment.application.service.convert;

import com.punks.recruitment.application.port.out.clock.ClockPort;
import com.punks.recruitment.application.port.out.convert.ConversionRepository;
import com.punks.recruitment.application.port.out.outbox.OutboxPort;
import com.punks.recruitment.application.port.out.precision.PrecisionPolicyPort;
import com.punks.recruitment.application.service.convert.dto.ConversionResult;
import com.punks.recruitment.application.service.convert.mapper.ConversionMapper;
import com.punks.recruitment.application.service.route.RouteFinder;
import com.punks.recruitment.domain.convert.ConversionRecord;
import com.punks.recruitment.domain.convert.ConvertCommand;
import com.punks.recruitment.domain.id.CommandId;
import com.punks.recruitment.domain.id.IdempotencyKey;
import com.punks.recruitment.domain.money.Amount;
import com.punks.recruitment.domain.money.Rate;
import com.punks.recruitment.domain.route.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ConvertTokenServiceTest {

	private ConversionRepository conversions;
	private OutboxPort outbox;
	private ClockPort clock;
	private PrecisionPolicyPort precision;
	private RouteFinder routeFinder;

	private ConvertTokenService service;

	@BeforeEach
	void setup() {
		conversions = mock(ConversionRepository.class);
		outbox = mock(OutboxPort.class);
		clock = mock(ClockPort.class);
		precision = mock(PrecisionPolicyPort.class);
		routeFinder = mock(RouteFinder.class);
		service = new ConvertTokenService(conversions, outbox, clock, precision, routeFinder);
	}

	private static ConversionRecord recordSucceeded(ConvertCommand cmd, Route route, Amount amount, Instant now) {
		return ConversionRecord.succeeded(cmd, amount, route, now);
	}

	private static ConversionRecord recordFailed(ConvertCommand cmd, Instant now) {
		return ConversionRecord.failedNoRoute(cmd, now);
	}

	@Test
	void shouldReturnExistingConversionIfAlreadyStored() {
		// given
		Instant now = Instant.parse("2024-01-01T00:00:00Z");
		ConvertCommand cmd = new ConvertCommand(
				new CommandId(),
				Optional.of(new IdempotencyKey("idem-key")),
				new Token("BTC"),
				new Token("ETH"),
				new Amount(BigDecimal.ONE),
				now
		);
		ConversionRecord existing = recordSucceeded(cmd, new Route(java.util.List.of()), new Amount(BigDecimal.ONE), now);

		when(conversions.findByCommand(cmd.commandId())).thenReturn(Mono.just(Optional.of(existing)));
		when(conversions.findByIdempotency(new IdempotencyKey("idem-key"))).thenReturn(Mono.just(Optional.empty()));

		// when
		Mono<ConversionResult> resultMono = service.convert(cmd);

		// then
		StepVerifier.create(resultMono)
				.expectNext(ConversionMapper.toResult(existing))
				.verifyComplete();

		verifyNoInteractions(routeFinder);
		verify(conversions, never()).saveIfAbsent(any());
	}


	@Test
	void shouldPublishEventWhenRecordIsNew() {
		// given
		Token from = new Token("BTC");
		Token to = new Token("ETH");
		Instant now = Instant.parse("2024-01-01T00:00:00Z");

		ConvertCommand cmd = new ConvertCommand(new CommandId(), Optional.empty(), from, to, new Amount(BigDecimal.TEN), now);
		Route route = new Route(List.of(new Hop(from, to, new Rate(BigDecimal.valueOf(2)), now)));

		when(conversions.findByCommand(any())).thenReturn(Mono.just(Optional.empty()));
		when(conversions.findByIdempotency(any())).thenReturn(Mono.just(Optional.empty()));
		when(routeFinder.findRoute(from, to)).thenReturn(Mono.just(route));
		when(precision.scaleFor(any())).thenReturn(2);
		when(precision.mathContext()).thenReturn(new MathContext(10));
		when(clock.now()).thenReturn(now);
		when(conversions.saveIfAbsent(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
		when(outbox.append(any(TokenConvertedEvent.class))).thenReturn(Mono.empty());

		// when
		Mono<ConversionResult> resultMono = service.convert(cmd);

		// then
		StepVerifier.create(resultMono)
				.assertNext(result -> {
					assertThat(result.tokenFrom()).isEqualTo(from);
					assertThat(result.tokenTo()).isEqualTo(to);
					assertThat(result.amountIn().value()).isEqualByComparingTo("10");
					assertThat(result.amountOut().value()).isEqualByComparingTo("20");
					assertThat(result.route().hops()).hasSize(1);
				})
				.verifyComplete();
		verify(conversions).saveIfAbsent(any());
		verify(outbox).append(any(TokenConvertedEvent.class));
	}

	@Test
	void shouldNotPublishEventWhenRecordAlreadyExists() {
		// given
		Token from = new Token("BTC");
		Token to = new Token("ETH");
		Instant now = Instant.parse("2024-01-01T00:00:00Z");

		ConvertCommand cmd = new ConvertCommand(new CommandId(), Optional.empty(), from, to, new Amount(BigDecimal.ONE), now);
		Route route = new Route(List.of(new Hop(from, to, new Rate(BigDecimal.ONE), now)));

		ConversionRecord attempted = recordSucceeded(cmd, route, new Amount(BigDecimal.ONE), now);
		ConversionRecord existing = recordSucceeded(cmd, route, new Amount(BigDecimal.ONE), now);

		when(conversions.findByCommand(any())).thenReturn(Mono.just(Optional.empty()));
		when(conversions.findByIdempotency(any())).thenReturn(Mono.just(Optional.empty()));
		when(routeFinder.findRoute(any(), any())).thenReturn(Mono.just(route));
		when(precision.scaleFor(any())).thenReturn(2);
		when(precision.mathContext()).thenReturn(new MathContext(10));
		when(clock.now()).thenReturn(now);
		when(conversions.saveIfAbsent(any())).thenReturn(Mono.just(existing));

		// when
		Mono<ConversionResult> resultMono = service.convert(cmd);

		// then
		StepVerifier.create(resultMono)
				.expectNext(ConversionMapper.toResult(existing))
				.verifyComplete();

		verify(conversions).saveIfAbsent(any());
		verify(outbox, never()).append(any());
	}

	@Test
	void shouldSaveFailedRecordWhenNoRouteFound() {
		// given
		Token from = new Token("BTC");
		Token to = new Token("DOGE");
		Instant now = Instant.parse("2024-01-01T00:00:00Z");
		ConvertCommand cmd = new ConvertCommand(new CommandId(), Optional.empty(), from, to, new Amount(BigDecimal.ONE), now);
		ConversionRecord failed = recordFailed(cmd, now);

		when(conversions.findByCommand(any())).thenReturn(Mono.just(Optional.empty()));
		when(conversions.findByIdempotency(any())).thenReturn(Mono.just(Optional.empty()));
		when(routeFinder.findRoute(any(), any())).thenReturn(Mono.empty());
		when(clock.now()).thenReturn(now);
		when(conversions.saveIfAbsent(any())).thenReturn(Mono.just(failed));

		// when
		Mono<ConversionResult> resultMono = service.convert(cmd);

		// then
		StepVerifier.create(resultMono)
				.expectNext(ConversionMapper.toResult(failed))
				.verifyComplete();

		verify(conversions).saveIfAbsent(any());
		verifyNoInteractions(outbox);
	}

	@Test
	void shouldNotPublishOutboxEventWhenRecordAlreadyExists() {
		// given
		Token from = new Token("BTC");
		Token to = new Token("ETH");
		Instant now = Instant.parse("2024-01-01T00:00:00Z");
		ConvertCommand cmd = new ConvertCommand(new CommandId(), Optional.empty(), from, to, new Amount(BigDecimal.ONE), now);
		Route route = new Route(java.util.List.of(new Hop(from, to, new Rate(BigDecimal.ONE), now)));
		ConversionRecord existing = recordSucceeded(cmd, route, new Amount(BigDecimal.ONE), now);

		when(conversions.findByCommand(any())).thenReturn(Mono.just(Optional.empty()));
		when(conversions.findByIdempotency(any())).thenReturn(Mono.just(Optional.empty()));
		when(routeFinder.findRoute(any(), any())).thenReturn(Mono.just(route));
		when(precision.scaleFor(any())).thenReturn(2);
		when(precision.mathContext()).thenReturn(new MathContext(10));
		when(clock.now()).thenReturn(now);
		when(conversions.saveIfAbsent(any())).thenReturn(Mono.just(existing));

		// when
		Mono<ConversionResult> resultMono = service.convert(cmd);

		// then
		StepVerifier.create(resultMono)
				.expectNext(ConversionMapper.toResult(existing))
				.verifyComplete();

		verify(outbox, never()).append(any());
	}
}
