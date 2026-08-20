package com.aegisterra.platform.domain.events;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PlatformDomainEventTest {

    @Test
    void buildsCanonicalEnvelope() {
        UUID subjectId = UUID.randomUUID();
        PlatformDomainEvent event = PlatformDomainEvent.of(
            DomainEventTypes.TASK_CREATED,
            UUID.randomUUID(),
            "GENERIC_SUBJECT",
            subjectId,
            "corr-1",
            Map.of("taskId", "t1")
        );
        assertNotNull(event.eventId());
        assertEquals(DomainEventTypes.TASK_CREATED, event.eventType());
        assertEquals(subjectId, event.subjectId());
        assertEquals("corr-1", event.correlationId());
        assertEquals("t1", event.payloadString("taskId"));
    }
}
