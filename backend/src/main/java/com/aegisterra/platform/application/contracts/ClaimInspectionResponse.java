package com.aegisterra.platform.application.contracts;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ClaimInspectionResponse(
    UUID id,
    UUID claimId,
    UUID inspectorId,
    Instant scheduledAt,
    Instant completedAt,
    BigDecimal checkInLatitude,
    BigDecimal checkInLongitude,
    String findingsJson,
    String checklistJson,
    String notes,
    String status,
    Instant createdAt
) {}
