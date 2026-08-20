package com.aegisterra.platform.application.contracts;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record WorkflowInstanceResponse(
    UUID id,
    UUID definitionId,
    UUID definitionVersionId,
    int definitionVersionNo,
    String subjectType,
    UUID subjectId,
    String currentStepCode,
    String status,
    String correlationId,
    String payloadJson,
    Instant startedAt,
    Instant completedAt,
    List<WorkflowEventResponse> events,
    List<WorkflowTransitionLogResponse> transitions
) {}
