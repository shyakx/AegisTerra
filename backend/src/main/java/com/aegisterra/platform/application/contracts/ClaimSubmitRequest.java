package com.aegisterra.platform.application.contracts;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ClaimSubmitRequest(
    @NotNull UUID policyId,
    @NotBlank String claimTypeCode,
    @NotNull LocalDate incidentDate,
    @NotBlank String description,
    @NotNull @DecimalMin("0.0") BigDecimal claimedAmount,
    String currency,
    UUID farmId,
    UUID seasonId,
    UUID cropId,
    String causeOfLoss,
    String correlationId
) {}
