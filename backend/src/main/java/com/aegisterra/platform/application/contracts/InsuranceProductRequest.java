package com.aegisterra.platform.application.contracts;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record InsuranceProductRequest(
    @NotBlank @Size(max = 64) String code,
    @NotBlank @Size(max = 255) String name,
    @Size(max = 2000) String description,
    @NotBlank String pricingStrategyCode,
    String eligibilityJson,
    String configJson,
    String status
) {}
