package com.aegisterra.platform.application.contracts;

import java.math.BigDecimal;
import java.util.UUID;

public record CropSeasonResponse(
    UUID id,
    UUID farmId,
    UUID plotId,
    UUID cropId,
    UUID seasonId,
    BigDecimal plantedAreaHa,
    BigDecimal yieldTHa,
    String status
) {}
