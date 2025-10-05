package com.punks.recruitment.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "precision.math")
@Data
public class PrecisionProperties {
	private int context;
	private int scale;
}
