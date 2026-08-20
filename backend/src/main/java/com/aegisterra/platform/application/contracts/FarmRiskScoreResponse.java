package com.aegisterra.platform.application.contracts;

import java.time.Instant;
import java.util.UUID;

public record FarmRiskScoreResponse(
    UUID id,
    UUID farmId,
    double score,
    Double confidence,
    String grade,
    Instant windowStart,
    Instant windowEnd,
    String componentsJson,
    String ruleSetCode,
    String ruleVersion,
    String modelVersion,
    Instant calculatedAt
) {}
