package com.aegisterra.platform.application.contracts;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PremiumQuoteResponse(
    UUID id,
    UUID productId,
    UUID coveragePackageId,
    UUID farmerId,
    UUID farmId,
    UUID cropId,
    UUID seasonId,
    String currency,
    BigDecimal baseAmount,
    BigDecimal grossAmount,
    BigDecimal netAmount,
    BigDecimal coverageAmount,
    String breakdownJson,
    String factorsHash,
    Instant expiresAt,
    String status
) {}
