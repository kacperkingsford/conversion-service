package com.punks.recruitment.application.service.convert.dto;

import java.time.Duration;

public record RouteQuery(int maxHops, Duration maxPriceAge) {
}
