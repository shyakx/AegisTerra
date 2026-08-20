package com.aegisterra.platform.application.contracts;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record FarmBoundaryRequest(
    @NotNull UUID farmId,
    @NotBlank String geoJson,
    String source,
    String status,
    String reason
) {}
