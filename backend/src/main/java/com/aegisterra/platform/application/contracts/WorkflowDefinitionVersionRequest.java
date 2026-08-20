package com.aegisterra.platform.application.contracts;

import jakarta.validation.constraints.NotBlank;

public record WorkflowDefinitionVersionRequest(
    @NotBlank String graphJson
) {}
