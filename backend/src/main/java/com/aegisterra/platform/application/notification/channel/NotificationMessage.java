package com.aegisterra.platform.application.notification.channel;

import java.util.Map;
import java.util.UUID;

public record NotificationMessage(
    UUID recipientUserId,
    String title,
    String body,
    String eventType,
    UUID eventId,
    String subjectType,
    UUID subjectId,
    String correlationId,
    String templateCode,
    Map<String, Object> payload
) {}
