package com.aegisterra.platform.application.contracts;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record PolicyTypeRequest(
    @NotBlank @Size(max = 64) String code,
    @NotBlank @Size(max = 255) String name,
    @Size(max = 1000) String description,
    String coverageRulesJson,
    UUID productId,
    String status
) {}
