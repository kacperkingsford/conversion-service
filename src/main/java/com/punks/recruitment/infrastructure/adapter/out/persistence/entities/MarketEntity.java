package com.punks.recruitment.infrastructure.adapter.out.persistence.entities;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Table("markets")
public class MarketEntity {
	@Id
	@Column("market_id")
	private String marketId; // for ex. USD-GBP

	@Column("base_token")
	private String baseToken;

	@Column("quote_token")
	private String quoteToken;

	@Column("price")
	private BigDecimal price;

	@Column("enabled")
	private Boolean enabled;

	@Column("updated_at")
	private Instant updatedAt;

	public String getMarketId() {
		return marketId;
	}

	public void setMarketId(String marketId) {
		this.marketId = marketId;
	}

	public String getBaseToken() {
		return baseToken;
	}

	public void setBaseToken(String baseToken) {
		this.baseToken = baseToken;
	}

	public String getQuoteToken() {
		return quoteToken;
	}

	public void setQuoteToken(String quoteToken) {
		this.quoteToken = quoteToken;
	}

	public BigDecimal getPrice() {
		return price;
	}

	public void setPrice(BigDecimal price) {
		this.price = price;
	}

	public Boolean getEnabled() {
		return enabled;
	}

	public void setEnabled(Boolean enabled) {
		this.enabled = enabled;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(Instant updatedAt) {
		this.updatedAt = updatedAt;
	}
}
