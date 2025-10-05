package com.punks.recruitment.infrastructure.adapter.out.persistence.entities;

import io.r2dbc.postgresql.codec.Json;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Table("conversions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversionEntity implements Persistable<UUID> {
	@Id
	@Column("conversion_id")
	private UUID conversionId;

	@Column("command_id")
	private UUID commandId;

	@Column("idempotency_key")
	private String idempotencyKey;

	@Column("token_from")
	private String tokenFrom;

	@Column("token_to")
	private String tokenTo;

	@Column("amount_in")
	private BigDecimal amountIn;

	@Column("amount_out")
	private BigDecimal amountOut;

	@Column("path_json")
	private Json pathJson; // conversion path, for ex. from = EUR, to = USD, path: EUR -> GBP -> USD

	@Column("computed_at")
	private Instant computedAt;

	@Column("status")
	private String status;

	@Transient
	private boolean isNew = false;

	public void markAsNew() {
		this.isNew = true;
	}

	@Override
	public UUID getId() {
		return conversionId;
	}

	@Override
	public boolean isNew() {
		return isNew;
	}
}
