package com.aegisterra.platform.application.contracts;

import java.time.Instant;
import java.util.UUID;

public record WorkflowEventResponse(
    UUID id,
    String eventType,
    String fromStepCode,
    String toStepCode,
    String actionCode,
    UUID actorId,
    String message,
    Instant occurredAt
) {}
