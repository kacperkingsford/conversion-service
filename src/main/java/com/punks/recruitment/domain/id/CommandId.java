package com.punks.recruitment.domain.id;

import java.util.UUID;

public record CommandId(UUID value) {
	public CommandId() {
		this(UUID.randomUUID());
	}
}
