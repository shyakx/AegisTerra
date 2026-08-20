package com.aegisterra.platform.application.contracts;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CoverageLimitRequest(
    @NotBlank String perilCode,
    @NotNull BigDecimal limitAmount,
    @NotBlank String currency
) {}
