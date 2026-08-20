package com.aegisterra.platform.application.contracts;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record RegistrationDraftRequest(
    @Min(1) @Max(8) int currentStep,
    @NotBlank String payloadJson
) {}
