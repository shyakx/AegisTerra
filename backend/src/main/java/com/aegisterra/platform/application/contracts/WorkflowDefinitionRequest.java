package com.aegisterra.platform.application.contracts;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public record WorkflowDefinitionRequest(
    @NotBlank String code,
    @NotBlank String name,
    String description,
    @NotBlank String graphJson
) {}
