package com.aegisterra.platform.application.contracts;

import java.util.UUID;

public record ClaimTypeResponse(
    UUID id,
    String code,
    String name,
    String description,
    String nature,
    String workflowDefinitionCode,
    String assessmentProfileJson,
    boolean requiresGeo,
    String status
) {}
