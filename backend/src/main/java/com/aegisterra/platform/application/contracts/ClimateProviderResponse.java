package com.aegisterra.platform.application.contracts;

import java.util.UUID;

public record ClimateProviderResponse(
    UUID id,
    String code,
    String displayName,
    boolean enabled,
    String capabilities,
    String status
) {}
