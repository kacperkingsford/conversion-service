package com.punks.recruitment.domain.route;

import com.punks.recruitment.domain.money.Rate;

import java.time.Instant;

public record Hop(Token from, Token to, Rate rate, Instant quotedAt) {}
