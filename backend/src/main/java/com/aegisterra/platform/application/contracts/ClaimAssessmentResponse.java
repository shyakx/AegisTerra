package com.aegisterra.platform.application.contracts;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ClaimAssessmentResponse(
    UUID id,
    UUID claimId,
    UUID inspectionId,
    String methodsJson,
    String findingsJson,
    BigDecimal recommendedAmount,
    BigDecimal assessedAmount,
    String currency,
    BigDecimal confidence,
    String fraudHintsJson,
    UUID assessorId,
    boolean accepted,
    String notes,
    Instant assessedAt
) {}
