package com.aegisterra.platform.application.contracts;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record WorkflowTaskResponse(
    UUID id,
    UUID instanceId,
    UUID workflowStepId,
    String stepCode,
    String taskType,
    String title,
    String description,
    String subjectType,
    UUID subjectId,
    UUID assigneeUserId,
    String assigneeRoleCode,
    int priority,
    Instant dueAt,
    String status,
    String outcome,
    Instant completedAt,
    UUID completedBy,
    Instant createdAt,
    List<TaskAssignmentResponse> assignments,
    List<TaskCommentResponse> comments,
    List<WorkflowEventResponse> timeline
) {}
