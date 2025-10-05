package com.punks.recruitment.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "route")
@Data
public class RouteQueryProperties {
	private int maxHops;
	private Duration maxPriceAge;
}
