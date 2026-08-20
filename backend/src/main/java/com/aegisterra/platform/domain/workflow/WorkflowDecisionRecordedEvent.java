package com.aegisterra.platform.domain.workflow;

import java.util.UUID;

/**
 * Published after a decision is successfully applied.
 */
public record WorkflowDecisionRecordedEvent(
    UUID decisionId,
    UUID taskId,
    UUID instanceId,
    String decisionTypeCode,
    String outcomeCode,
    String effect,
    String workflowAction,
    UUID actorId
) {}
