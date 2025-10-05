package com.punks.recruitment.application.port.out.outbox;

import java.util.UUID;

public record OutboxRecord(UUID eventId, String payloadJson) {}
