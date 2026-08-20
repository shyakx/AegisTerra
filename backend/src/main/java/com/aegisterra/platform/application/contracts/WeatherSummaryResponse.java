package com.aegisterra.platform.application.contracts;

import java.time.Instant;
import java.util.UUID;

public record WeatherSummaryResponse(
    UUID farmId,
    Instant from,
    Instant to,
    Double rainfallTotalMm,
    Double meanTempC,
    Integer dryDays,
    Integer wetDays,
    String narrative,
    String metricsJson
) {}
