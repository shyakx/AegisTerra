package com.aegisterra.platform.application.contracts;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record HouseholdRequest(
    @NotBlank @Size(max = 64) String code,
    @Size(max = 255) String headName
) {}
