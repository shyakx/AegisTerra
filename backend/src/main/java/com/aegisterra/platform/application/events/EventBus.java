package com.aegisterra.platform.application.events;

import com.aegisterra.platform.domain.events.DomainEvent;

/**
 * Port for publishing domain events. Spring in-process now; Kafka/RabbitMQ later.
 */
public interface EventBus {
    void publish(DomainEvent event);
}
