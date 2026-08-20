package com.aegisterra.platform.application.contracts;

import jakarta.validation.constraints.NotBlank;

public record NotificationPreferenceRequest(
    @NotBlank String channel,
    @NotBlank String eventType,
    boolean enabled
) {}
