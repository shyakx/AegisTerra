package com.aegisterra.platform.application.contracts;

import java.time.Instant;
import java.util.UUID;

public record AuditLogResponse(
    UUID id,
    String action,
    UUID actorUserId,
    String resourceType,
    String resourceId,
    String correlationId,
    String detailsJson,
    Instant createdAt
) {
}
