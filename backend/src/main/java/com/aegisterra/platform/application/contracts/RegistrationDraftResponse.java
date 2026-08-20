package com.aegisterra.platform.application.contracts;

import java.time.Instant;
import java.util.UUID;

public record RegistrationDraftResponse(
    UUID id,
    UUID createdByUserId,
    UUID farmerId,
    int currentStep,
    String payloadJson,
    String status,
    Instant expiresAt,
    Instant updatedAt
) {}
