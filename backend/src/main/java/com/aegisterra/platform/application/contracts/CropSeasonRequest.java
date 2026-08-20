package com.aegisterra.platform.application.contracts;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record CropSeasonRequest(
    @NotNull UUID farmId,
    UUID plotId,
    @NotNull UUID cropId,
    @NotNull UUID seasonId,
    BigDecimal plantedAreaHa,
    String status,
    String reason
) {}
