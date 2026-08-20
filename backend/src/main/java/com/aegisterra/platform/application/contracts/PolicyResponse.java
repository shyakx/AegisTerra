package com.aegisterra.platform.application.contracts;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record PolicyResponse(
    UUID id,
    String policyNumber,
    UUID farmerId,
    UUID farmId,
    UUID policyTypeId,
    UUID productId,
    UUID coveragePackageId,
    UUID cropId,
    UUID seasonId,
    UUID premiumQuoteId,
    UUID parentPolicyId,
    UUID insuranceCompanyId,
    BigDecimal coverageAmount,
    BigDecimal premiumAmount,
    String currency,
    LocalDate startDate,
    LocalDate endDate,
    Instant issuedAt,
    String status,
    String transitionReason
) {}
