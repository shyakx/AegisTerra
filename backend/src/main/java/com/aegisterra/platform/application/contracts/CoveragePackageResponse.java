package com.aegisterra.platform.application.contracts;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CoveragePackageResponse(
    UUID id,
    UUID productId,
    UUID policyTypeId,
    String code,
    String name,
    BigDecimal coverageLevelPct,
    BigDecimal maxSumInsured,
    String configJson,
    String status,
    List<CoverageLimitResponse> limits,
    List<ExclusionResponse> exclusions,
    List<WaitingPeriodResponse> waitingPeriods
) {}
