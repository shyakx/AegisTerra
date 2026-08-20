package com.aegisterra.platform.application.contracts;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CoveragePackageRequest(
    @NotNull UUID productId,
    UUID policyTypeId,
    @NotBlank @Size(max = 64) String code,
    @NotBlank @Size(max = 255) String name,
    BigDecimal coverageLevelPct,
    BigDecimal maxSumInsured,
    String configJson,
    String status,
    List<CoverageLimitRequest> limits,
    List<ExclusionRequest> exclusions,
    List<WaitingPeriodRequest> waitingPeriods
) {}
