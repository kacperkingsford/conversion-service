package com.punks.recruitment.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "topics")
@Data
public class TopicsProperties {
	private String convertCommands;
	private String marketEvents;
	private String conversionEvents;
	private String convertCommandsDlq;
	private String marketEventsDlq;
}
