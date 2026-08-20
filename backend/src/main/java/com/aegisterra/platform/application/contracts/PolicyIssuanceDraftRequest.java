package com.aegisterra.platform.application.contracts;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record PolicyIssuanceDraftRequest(
    @Min(1) @Max(10) int currentStep,
    @NotBlank String payloadJson
) {}
