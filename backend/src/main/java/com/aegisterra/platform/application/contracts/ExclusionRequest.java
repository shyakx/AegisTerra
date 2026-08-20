package com.aegisterra.platform.application.contracts;

import jakarta.validation.constraints.NotBlank;

public record ExclusionRequest(
    @NotBlank String code,
    @NotBlank String description
) {}
