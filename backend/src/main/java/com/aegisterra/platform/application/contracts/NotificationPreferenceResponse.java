package com.aegisterra.platform.application.contracts;

public record NotificationPreferenceResponse(
    String channel,
    String eventType,
    boolean enabled
) {}
