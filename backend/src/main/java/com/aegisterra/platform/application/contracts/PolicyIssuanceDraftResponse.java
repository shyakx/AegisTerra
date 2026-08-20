package com.aegisterra.platform.application.contracts;

import java.time.Instant;
import java.util.UUID;

public record PolicyIssuanceDraftResponse(
    UUID id,
    UUID createdByUserId,
    UUID policyId,
    int currentStep,
    String payloadJson,
    String status,
    Instant expiresAt,
    Instant updatedAt
) {}
