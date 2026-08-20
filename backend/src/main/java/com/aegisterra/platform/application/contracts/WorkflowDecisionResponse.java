package com.aegisterra.platform.application.contracts;

import java.time.Instant;
import java.util.UUID;

public record WorkflowDecisionResponse(
    UUID id,
    UUID taskId,
    UUID instanceId,
    String stepCode,
    String decisionTypeCode,
    String outcomeCode,
    String effect,
    String workflowAction,
    String comment,
    UUID targetUserId,
    String subjectType,
    UUID subjectId,
    UUID decidedBy,
    Instant decidedAt,
    String status
) {}
