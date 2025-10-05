package com.punks.recruitment.infrastructure.adapter.out.persistence.entities;

import io.r2dbc.postgresql.codec.Json;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Table("conversions")
public class ConversionEntity {
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

	public UUID getConversionId() {
		return conversionId;
	}

	public void setConversionId(UUID conversionId) {
		this.conversionId = conversionId;
	}

	public UUID getCommandId() {
		return commandId;
	}

	public void setCommandId(UUID commandId) {
		this.commandId = commandId;
	}

	public String getIdempotencyKey() {
		return idempotencyKey;
	}

	public void setIdempotencyKey(String idempotencyKey) {
		this.idempotencyKey = idempotencyKey;
	}

	public String getTokenFrom() {
		return tokenFrom;
	}

	public void setTokenFrom(String tokenFrom) {
		this.tokenFrom = tokenFrom;
	}

	public String getTokenTo() {
		return tokenTo;
	}

	public void setTokenTo(String tokenTo) {
		this.tokenTo = tokenTo;
	}

	public BigDecimal getAmountIn() {
		return amountIn;
	}

	public void setAmountIn(BigDecimal amountIn) {
		this.amountIn = amountIn;
	}

	public BigDecimal getAmountOut() {
		return amountOut;
	}

	public void setAmountOut(BigDecimal amountOut) {
		this.amountOut = amountOut;
	}

	public Json getPathJson() {
		return pathJson;
	}

	public void setPathJson(Json pathJson) {
		this.pathJson = pathJson;
	}

	public Instant getComputedAt() {
		return computedAt;
	}

	public void setComputedAt(Instant computedAt) {
		this.computedAt = computedAt;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}
}
