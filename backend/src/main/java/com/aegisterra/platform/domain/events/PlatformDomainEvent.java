package com.aegisterra.platform.domain.events;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Immutable platform domain event used by the EventBus.
 */
public record PlatformDomainEvent(
    UUID eventId,
    String eventType,
    Instant occurredAt,
    UUID actorId,
    String subjectType,
    UUID subjectId,
    String correlationId,
    Map<String, Object> payload
) implements DomainEvent {

    public PlatformDomainEvent {
        payload = payload == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(payload));
    }

    public static PlatformDomainEvent of(
        String eventType,
        UUID actorId,
        String subjectType,
        UUID subjectId,
        String correlationId,
        Map<String, Object> payload
    ) {
        return new PlatformDomainEvent(
            UUID.randomUUID(),
            eventType,
            Instant.now(),
            actorId,
            subjectType == null ? "UNKNOWN" : subjectType,
            subjectId,
            correlationId,
            payload
        );
    }

    public Object payloadValue(String key) {
        return payload.get(key);
    }

    public String payloadString(String key) {
        Object value = payload.get(key);
        return value == null ? null : String.valueOf(value);
    }
}
