package com.aegisterra.platform.application.contracts;

import jakarta.validation.constraints.NotBlank;

public record WorkflowTransitionRequest(
    @NotBlank String action,
    String reason
) {}
