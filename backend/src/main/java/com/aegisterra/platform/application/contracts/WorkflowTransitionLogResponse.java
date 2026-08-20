package com.aegisterra.platform.application.contracts;

import java.time.Instant;
import java.util.UUID;

public record WorkflowTransitionLogResponse(
    UUID id,
    String fromStepCode,
    String toStepCode,
    String actionCode,
    UUID actorId,
    String reason,
    Instant occurredAt
) {}
