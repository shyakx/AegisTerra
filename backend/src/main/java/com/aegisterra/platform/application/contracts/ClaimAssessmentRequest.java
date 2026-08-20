package com.aegisterra.platform.application.contracts;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record ClaimAssessmentRequest(
    @NotBlank String methodsJson,
    String findingsJson,
    @NotNull @DecimalMin("0.0") BigDecimal recommendedAmount,
    String currency,
    BigDecimal confidence,
    String fraudHintsJson,
    UUID inspectionId,
    String notes,
    Boolean accepted
) {}
