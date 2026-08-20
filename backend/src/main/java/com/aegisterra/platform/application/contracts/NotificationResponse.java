package com.aegisterra.platform.application.contracts;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
    UUID id,
    String channel,
    String title,
    String body,
    String eventType,
    String subjectType,
    UUID subjectId,
    String correlationId,
    String templateCode,
    Instant sentAt,
    Instant readAt,
    String status,
    Instant createdAt
) {}
