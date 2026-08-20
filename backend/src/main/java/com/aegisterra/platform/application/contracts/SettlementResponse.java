package com.aegisterra.platform.application.contracts;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record SettlementResponse(
    UUID id,
    String settlementNumber,
    String sourceModule,
    UUID sourceRecordId,
    String sourceReference,
    BigDecimal amount,
    String currency,
    BigDecimal exchangeRate,
    String paymentMethod,
    String providerCode,
    String providerReference,
    String beneficiaryName,
    String beneficiaryAccount,
    String financialSnapshotJson,
    UUID workflowInstanceId,
    String workflowDefinitionCode,
    String correlationId,
    String reasonCode,
    String failureReason,
    Instant submittedAt,
    Instant completedAt,
    String status,
    Instant createdAt,
    Instant updatedAt
) {}
