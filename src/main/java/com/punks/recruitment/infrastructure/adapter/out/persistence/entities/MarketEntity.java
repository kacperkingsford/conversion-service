package com.punks.recruitment.infrastructure.adapter.out.persistence.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Table("markets")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
}
