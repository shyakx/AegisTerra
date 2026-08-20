package com.aegisterra.platform.domain.events;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Canonical domain event envelope. Transport-agnostic (Spring / Kafka / RabbitMQ).
 */
public interface DomainEvent {
    UUID eventId();
    String eventType();
    Instant occurredAt();
    UUID actorId();
    String subjectType();
    UUID subjectId();
    String correlationId();
    Map<String, Object> payload();
}
