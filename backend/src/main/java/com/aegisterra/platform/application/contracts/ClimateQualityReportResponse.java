package com.aegisterra.platform.application.contracts;

import java.time.Instant;
import java.util.UUID;

public record ClimateQualityReportResponse(
    UUID id,
    String scopeType,
    String scopeId,
    Instant periodStart,
    Instant periodEnd,
    String metricsJson,
    String overallGrade,
    Instant generatedAt
) {}
