package com.aegisterra.platform.application.contracts;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ClaimResponse(
    UUID id,
    String claimNumber,
    UUID policyId,
    String claimTypeCode,
    UUID farmerId,
    UUID farmId,
    UUID seasonId,
    UUID cropId,
    String causeOfLoss,
    String description,
    BigDecimal claimedAmount,
    BigDecimal assessedAmount,
    BigDecimal approvedAmount,
    String currency,
    LocalDate incidentDate,
    String status,
    String coverageSnapshotJson,
    String financialSnapshotJson,
    UUID workflowInstanceId,
    String workflowDefinitionCode,
    String fraudTier,
    BigDecimal fraudScore,
    Instant submittedAt,
    Instant closedAt,
    String reasonCode,
    String correlationId
) {}
