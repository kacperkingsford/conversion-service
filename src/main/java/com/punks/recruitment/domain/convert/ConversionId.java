package com.punks.recruitment.domain.convert;

import java.util.UUID;

public record ConversionId(UUID value) {
	public ConversionId() {
		this(UUID.randomUUID());
	}
}
