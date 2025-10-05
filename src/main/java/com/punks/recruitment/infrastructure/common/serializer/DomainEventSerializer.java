package com.punks.recruitment.infrastructure.common.serializer;

public interface DomainEventSerializer {
    String serialize(Object event);
}
