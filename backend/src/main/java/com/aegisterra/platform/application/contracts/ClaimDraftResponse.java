package com.aegisterra.platform.application.contracts;

import java.time.Instant;
import java.util.UUID;

public record ClaimDraftResponse(
    UUID id,
    UUID ownerUserId,
    UUID claimId,
    int currentStep,
    String payloadJson,
    String status,
    Instant expiresAt,
    Instant updatedAt
) {}
