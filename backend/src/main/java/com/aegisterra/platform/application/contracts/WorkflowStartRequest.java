package com.aegisterra.platform.application.contracts;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record WorkflowStartRequest(
    @NotNull UUID definitionId,
    @NotBlank String subjectType,
    @NotNull UUID subjectId,
    String correlationId,
    String payloadJson
) {}
