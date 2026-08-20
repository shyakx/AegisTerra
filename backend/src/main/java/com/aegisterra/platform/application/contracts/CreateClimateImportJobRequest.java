package com.aegisterra.platform.application.contracts;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record CreateClimateImportJobRequest(
    String providerCode,
    String jobType,
    String stationCode,
    List<ObservationRow> observations,
    String csvContent
) {
    public record ObservationRow(
        Instant observedAt,
        String variableCode,
        BigDecimal value,
        String unit
    ) {}
}
