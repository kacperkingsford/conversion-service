package com.punks.recruitment.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "topics")
public class TopicsProperties {
	private String convertCommands;
	private String marketEvents;
	private String conversionEvents;
	private String convertCommandsDlq;
	private String marketEventsDlq;

	public String getConvertCommands() {
		return convertCommands;
	}

	public void setConvertCommands(String convertCommands) {
		this.convertCommands = convertCommands;
	}

	public String getMarketEvents() {
		return marketEvents;
	}

	public void setMarketEvents(String marketEvents) {
		this.marketEvents = marketEvents;
	}

	public String getConversionEvents() {
		return conversionEvents;
	}

	public void setConversionEvents(String conversionEvents) {
		this.conversionEvents = conversionEvents;
	}

	public String getConvertCommandsDlq() {
		return convertCommandsDlq;
	}

	public void setConvertCommandsDlq(String convertCommandsDlq) {
		this.convertCommandsDlq = convertCommandsDlq;
	}

	public String getMarketEventsDlq() {
		return marketEventsDlq;
	}

	public void setMarketEventsDlq(String marketEventsDlq) {
		this.marketEventsDlq = marketEventsDlq;
	}
}
