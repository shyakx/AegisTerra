package com.aegisterra.platform.application.contracts;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record WaitingPeriodRequest(
    @NotBlank String code,
    @Min(0) int days,
    String description
) {}
