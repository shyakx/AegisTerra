package com.aegisterra.platform.application.contracts;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record SettlementCreateRequest(
    @NotBlank String sourceModule,
    @NotNull UUID sourceRecordId,
    String sourceReference,
    @NotNull @DecimalMin("0.0001") BigDecimal amount,
    @NotBlank String currency,
    BigDecimal exchangeRate,
    String paymentMethod,
    String providerCode,
    String beneficiaryName,
    String beneficiaryAccount,
    String financialSnapshotJson,
    String correlationId
) {}
