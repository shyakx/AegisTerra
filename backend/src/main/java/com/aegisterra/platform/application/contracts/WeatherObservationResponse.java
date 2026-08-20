package com.aegisterra.platform.application.contracts;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record WeatherObservationResponse(
    UUID id,
    UUID stationId,
    Instant observedAt,
    String variableCode,
    BigDecimal value,
    String unit,
    String qualityFlag,
    String providerCode,
    BigDecimal temperatureC,
    BigDecimal rainfallMm,
    BigDecimal humidityPct,
    BigDecimal windSpeedMs
) {}
