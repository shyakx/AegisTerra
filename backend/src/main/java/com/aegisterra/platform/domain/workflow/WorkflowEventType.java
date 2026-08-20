package com.aegisterra.platform.domain.workflow;

public enum WorkflowEventType {
    CREATED,
    STARTED,
    TRANSITIONED,
    DECISION,
    COMPLETED,
    REJECTED,
    CANCELLED
}
