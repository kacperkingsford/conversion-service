package com.punks.recruitment.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "route")
public class RouteQueryProperties {
	private int maxHops;
	private Duration maxPriceAge;

	public int getMaxHops() {
		return maxHops;
	}

	public void setMaxHops(int maxHops) {
		this.maxHops = maxHops;
	}

	public Duration getMaxPriceAge() {
		return maxPriceAge;
	}

	public void setMaxPriceAge(Duration maxPriceAge) {
		this.maxPriceAge = maxPriceAge;
	}
}
