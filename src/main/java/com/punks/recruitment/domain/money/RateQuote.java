package com.punks.recruitment.domain.money;

import java.time.Instant;

public record RateQuote(Rate rate, Instant quotedAt) {}
