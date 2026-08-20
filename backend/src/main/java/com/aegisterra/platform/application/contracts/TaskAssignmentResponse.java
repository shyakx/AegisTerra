package com.aegisterra.platform.application.contracts;

import java.time.Instant;
import java.util.UUID;

public record TaskAssignmentResponse(
    UUID id,
    String action,
    UUID fromUserId,
    UUID toUserId,
    String toRoleCode,
    UUID actorId,
    String reason,
    Instant occurredAt
) {}
