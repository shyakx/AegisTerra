package com.aegisterra.platform.application.contracts;

import java.time.Instant;
import java.util.UUID;

public record SeasonSummaryResponse(
    UUID farmId,
    String seasonCode,
    String grade,
    String metricsJson,
    String narrative,
    Instant generatedAt
) {}
