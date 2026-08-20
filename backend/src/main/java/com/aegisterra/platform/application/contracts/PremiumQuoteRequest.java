package com.aegisterra.platform.application.contracts;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record PremiumQuoteRequest(
    @NotNull UUID productId,
    @NotNull UUID coveragePackageId,
    @NotNull UUID farmerId,
    @NotNull UUID farmId,
    UUID cropId,
    UUID seasonId,
    UUID partnerId,
    BigDecimal areaHa,
    String riskZoneCode,
    BigDecimal weatherMultiplier
) {}
