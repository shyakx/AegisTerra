package com.aegisterra.platform.application.contracts;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record ClaimDraftRequest(
    @Min(1) @Max(20) int currentStep,
    @NotBlank String payloadJson
) {}
