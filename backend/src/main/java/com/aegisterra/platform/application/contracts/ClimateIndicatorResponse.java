package com.aegisterra.platform.application.contracts;

import java.time.Instant;
import java.util.UUID;

public record ClimateIndicatorResponse(
    UUID id,
    String indicatorType,
    String subjectType,
    String subjectId,
    String severity,
    Double indexValue,
    Instant windowStart,
    Instant windowEnd,
    String detailsJson,
    Instant calculatedAt
) {}
